package com.fiap.feedbacks.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.RuleTargetInput;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDK do caminho de relatório (FR-11/12/17 / AD-6 / AD-10 / AD-17).
 *
 * <p>EventBridge 08:00 {@code America/Sao_Paulo} (= 11:00 UTC) + S3 + Lambda report.
 * VPC (AD-17): só quando {@code FEEDBACKS_REPORT_VPC_ID} (e opcionalmente SG) forem
 * informados — caso contrário a Lambda fica fora de VPC (SES/S3 ok; RDS exige VPC
 * + SG → 5432 na conta demo). Sem ECS/ECR (FR-15).
 *
 * <p>Invoke manual (AD-10):
 * {@code aws lambda invoke --function-name feedbacks-lambda-report
 * --payload '{"periodo":"diario"}' out.json}
 */
public class RelatorioStack extends Stack {

    public RelatorioStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, RelatorioStackConfig.defaults());
    }

    public RelatorioStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final RelatorioStackConfig config) {
        super(scope, id, props);

        RelatorioStackConfig cfg = config == null ? RelatorioStackConfig.defaults() : config;

        Bucket.Builder bucketBuilder = Bucket.Builder.create(this, "RelatoriosBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(false);
        if (cfg.bucketName() != null && !cfg.bucketName().isBlank()) {
            bucketBuilder.bucketName(cfg.bucketName());
        }
        Bucket reportsBucket = bucketBuilder.build();

        ISecret adminEmailSecret = Secret.fromSecretNameV2(
                this, "AdminEmailSecret", cfg.adminEmailSecretName());

        String zipPath = cfg.functionZipPath() == null || cfg.functionZipPath().isBlank()
                ? Path.of("..", "apps", "report", "target", "function.zip").normalize().toString()
                : cfg.functionZipPath();

        String adminEmail = adminEmailSecret.secretValueFromJson("adminEmail").unsafeUnwrap();

        Map<String, String> env = new HashMap<>();
        env.put("QUARKUS_LAMBDA_HANDLER", "report");
        env.put("ADMIN_EMAIL", adminEmail);
        env.put("SES_FROM_EMAIL", adminEmail);
        env.put("REPORT_S3_BUCKET", reportsBucket.getBucketName());

        ISecret dbSecret = null;
        if (cfg.dbSecretName() != null && !cfg.dbSecretName().isBlank()) {
            dbSecret = Secret.fromSecretNameV2(this, "DbSecret", cfg.dbSecretName());
            env.put("DB_URL", dbSecret.secretValueFromJson("dbUrl").unsafeUnwrap());
            env.put("DB_USER", dbSecret.secretValueFromJson("dbUser").unsafeUnwrap());
            env.put("DB_PASSWORD", dbSecret.secretValueFromJson("dbPassword").unsafeUnwrap());
        }

        Function.Builder fnBuilder = Function.Builder.create(this, "ReportLambda")
                .functionName("feedbacks-lambda-report")
                .runtime(Runtime.JAVA_17)
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .code(Code.fromAsset(zipPath))
                .memorySize(1024)
                .timeout(Duration.seconds(120))
                .environment(env);

        if (cfg.vpcId() != null && !cfg.vpcId().isBlank()) {
            IVpc vpc = Vpc.fromLookup(this, "ReportVpc", VpcLookupOptions.builder()
                    .vpcId(cfg.vpcId())
                    .build());
            fnBuilder.vpc(vpc);
            fnBuilder.vpcSubnets(SubnetSelection.builder()
                    .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                    .build());
            if (cfg.lambdaSecurityGroupId() != null && !cfg.lambdaSecurityGroupId().isBlank()) {
                ISecurityGroup sg = SecurityGroup.fromSecurityGroupId(
                        this, "ReportLambdaSg", cfg.lambdaSecurityGroupId());
                fnBuilder.securityGroups(List.of(sg));
            }
        }

        Function reportFn = fnBuilder.build();

        adminEmailSecret.grantRead(reportFn);
        if (dbSecret != null) {
            dbSecret.grantRead(reportFn);
        }
        reportsBucket.grantPut(reportFn);

        reportFn.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("ses:SendEmail", "ses:SendRawEmail"))
                .resources(List.of("*"))
                .build());

        // 08:00 America/Sao_Paulo = 11:00 UTC (BRT UTC-3)
        Rule daily = Rule.Builder.create(this, "ReportDailyRule")
                .ruleName("feedbacks-report-diario")
                .description("Relatório diário 08:00 America/Sao_Paulo (cron 0 11 * * ? *)")
                .schedule(Schedule.cron(CronOptions.builder()
                        .minute("0")
                        .hour("11")
                        .build()))
                .build();
        daily.addTarget(LambdaFunction.Builder.create(reportFn)
                .event(RuleTargetInput.fromObject(Map.of("periodo", "diario")))
                .build());

        Rule weekly = Rule.Builder.create(this, "ReportWeeklyRule")
                .ruleName("feedbacks-report-semanal")
                .description("Relatório semanal segunda 08:00 America/Sao_Paulo")
                .schedule(Schedule.cron(CronOptions.builder()
                        .minute("0")
                        .hour("11")
                        .weekDay("MON")
                        .build()))
                .build();
        weekly.addTarget(LambdaFunction.Builder.create(reportFn)
                .event(RuleTargetInput.fromObject(Map.of("periodo", "semanal")))
                .build());

        CfnOutput.Builder.create(this, "ReportFunctionName")
                .value(reportFn.getFunctionName())
                .description("Invoke manual AD-10: payload {\"periodo\":\"diario\"|\"semanal\"}")
                .build();
        CfnOutput.Builder.create(this, "RelatoriosBucketName")
                .value(reportsBucket.getBucketName())
                .build();
        CfnOutput.Builder.create(this, "ReportVpcNote")
                .value(cfg.vpcId() == null || cfg.vpcId().isBlank()
                        ? "Lambda fora de VPC — defina FEEDBACKS_REPORT_VPC_ID (+ SG) para RDS (AD-17)"
                        : "Lambda na VPC " + cfg.vpcId())
                .build();
    }

    /**
     * Configuração — VPC/DB via env ou overrides (D7 / AD-17).
     */
    public record RelatorioStackConfig(
            String functionZipPath,
            String adminEmailSecretName,
            String dbSecretName,
            String bucketName,
            String vpcId,
            String lambdaSecurityGroupId) {

        public static RelatorioStackConfig defaults() {
            return new RelatorioStackConfig(
                    null,
                    "feedbacks/adminEmail",
                    blankToNull(System.getenv("FEEDBACKS_DB_SECRET_NAME")),
                    null,
                    blankToNull(System.getenv("FEEDBACKS_REPORT_VPC_ID")),
                    blankToNull(System.getenv("FEEDBACKS_REPORT_SG_ID")));
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
