package com.fiap.feedbacks.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
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
 * Demo: Lambda na VPC default (subnets públicas + {@code allowPublicSubnet}) com SG
 * dedicado autorizável no RDS — sem NAT. Invoke manual (AD-10) permanece.
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
            env.put("FEEDBACKS_REPORT_JDBC_ENABLED", "true");
        } else {
            env.put("FEEDBACKS_REPORT_JDBC_ENABLED", "false");
        }

        IVpc vpc = Vpc.fromLookup(this, "ReportVpc", VpcLookupOptions.builder()
                .isDefault(true)
                .build());
        SecurityGroup reportSg = SecurityGroup.Builder.create(this, "ReportLambdaSg")
                .vpc(vpc)
                .securityGroupName("feedbacks-report-lambda")
                .description("Lambda report - allowed source for RDS SG :5432")
                .allowAllOutbound(true)
                .build();

        Function.Builder fnBuilder = Function.Builder.create(this, "ReportLambda")
                .functionName("feedbacks-lambda-report")
                .runtime(Runtime.JAVA_17)
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .code(Code.fromAsset(zipPath))
                .memorySize(1024)
                .timeout(Duration.seconds(120))
                .environment(env)
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .allowPublicSubnet(true)
                .securityGroups(List.of(reportSg));

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
        CfnOutput.Builder.create(this, "ReportLambdaSecurityGroupId")
                .value(reportSg.getSecurityGroupId())
                .description("SG da Lambda report - autorizar no SG do RDS :5432")
                .build();
        CfnOutput.Builder.create(this, "ReportVpcNote")
                .value("Lambda na VPC default (public + allowPublicSubnet); SG " + reportSg.getSecurityGroupId())
                .build();
    }

    /**
     * Configuração — DB via env; VPC default sempre (AD-17 demo).
     */
    public record RelatorioStackConfig(
            String functionZipPath,
            String adminEmailSecretName,
            String dbSecretName,
            String bucketName) {

        public static RelatorioStackConfig defaults() {
            return new RelatorioStackConfig(
                    null,
                    "feedbacks/adminEmail",
                    blankToNull(System.getenv("FEEDBACKS_DB_SECRET_NAME")),
                    null);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
