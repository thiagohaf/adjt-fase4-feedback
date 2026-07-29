package com.fiap.feedbacks.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.Platform;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.HttpCodeTarget;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.sns.Subscription;
import software.amazon.awscdk.services.sns.SubscriptionProtocol;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDK da API em ECS Fargate + ALB + alarme SNS (FR-13/14/15, AD-1, AD-11, AD-17 demo).
 *
 * <p>Demo: VPC default (subnets públicas) + RDS com SG restrito ao ECS (e Lambda report).
 * Imagem via Docker asset a partir de {@code apps/api/Dockerfile}. HTTPS se
 * {@code FEEDBACKS_ACM_CERT_ARN} estiver definido.
 */
public class ApiStack extends Stack {

    public ApiStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, ApiStackConfig.defaults());
    }

    public ApiStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final ApiStackConfig config) {
        super(scope, id, props);

        ApiStackConfig cfg = config == null ? ApiStackConfig.defaults() : config;

        Repository repository = Repository.Builder.create(this, "ApiRepository")
                .repositoryName(cfg.ecrRepositoryName())
                .removalPolicy(RemovalPolicy.DESTROY)
                .emptyOnDelete(true)
                .build();

        IVpc vpc = Vpc.fromLookup(this, "DemoVpc", VpcLookupOptions.builder()
                .isDefault(true)
                .build());

        Cluster cluster = Cluster.Builder.create(this, "ApiCluster")
                .clusterName("feedbacks-api")
                .vpc(vpc)
                .containerInsightsV2(software.amazon.awscdk.services.ecs.ContainerInsights.DISABLED)
                .build();

        SecurityGroup ecsSg = SecurityGroup.Builder.create(this, "ApiEcsSg")
                .vpc(vpc)
                .securityGroupName("feedbacks-api-ecs")
                .description("ECS tasks da API — origem permitida no SG do RDS :5432")
                .allowAllOutbound(true)
                .build();

        ISecret jwtSecret = software.amazon.awscdk.services.secretsmanager.Secret
                .fromSecretNameV2(this, "JwtSecret", cfg.jwtSecretName());
        ISecret dbSecret = software.amazon.awscdk.services.secretsmanager.Secret
                .fromSecretNameV2(this, "DbSecret", cfg.dbSecretName());
        ISecret adminEmailSecret = software.amazon.awscdk.services.secretsmanager.Secret
                .fromSecretNameV2(this, "AdminEmailSecret", cfg.adminEmailSecretName());
        String adminEmail = adminEmailSecret.secretValueFromJson("adminEmail").unsafeUnwrap();

        String dockerDir = cfg.dockerContextPath() == null || cfg.dockerContextPath().isBlank()
                ? Path.of("..", "apps", "api").normalize().toString()
                : cfg.dockerContextPath();

        Map<String, String> environment = new HashMap<>();
        environment.put("QUARKUS_PROFILE", "aws");
        environment.put("QUARKUS_HTTP_HOST", "0.0.0.0");
        environment.put("FEEDBACKS_SQS_ALERT_QUEUE_URL", cfg.sqsAlertQueueUrl());

        Map<String, software.amazon.awscdk.services.ecs.Secret> secrets = new HashMap<>();
        secrets.put(
                "JWT_SECRET",
                software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(jwtSecret, "jwtSecret"));
        secrets.put(
                "DB_URL",
                software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(dbSecret, "dbUrl"));
        secrets.put(
                "DB_USERNAME",
                software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(dbSecret, "dbUser"));
        secrets.put(
                "DB_PASSWORD",
                software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(dbSecret, "dbPassword"));

        DockerImageAsset apiImage = DockerImageAsset.Builder.create(this, "ApiDockerImage")
                .directory(dockerDir)
                .platform(Platform.LINUX_AMD64)
                .build();

        boolean https = cfg.acmCertificateArn() != null && !cfg.acmCertificateArn().isBlank();
        ApplicationLoadBalancedFargateService.Builder serviceBuilder =
                ApplicationLoadBalancedFargateService.Builder.create(this, "ApiService")
                        .cluster(cluster)
                        .serviceName("feedbacks-api")
                        .cpu(256)
                        .memoryLimitMiB(1024)
                        .desiredCount(1)
                        .assignPublicIp(true)
                        .publicLoadBalancer(true)
                        .securityGroups(List.of(ecsSg))
                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromDockerImageAsset(apiImage))
                                .containerName("api")
                                .containerPort(8080)
                                .environment(environment)
                                .secrets(secrets)
                                .build())
                        .healthCheckGracePeriod(Duration.seconds(180));

        if (https) {
            ICertificate cert = Certificate.fromCertificateArn(
                    this, "ApiAcmCert", cfg.acmCertificateArn());
            serviceBuilder
                    .protocol(ApplicationProtocol.HTTPS)
                    .certificate(cert)
                    .redirectHTTP(true)
                    .listenerPort(443);
        } else {
            serviceBuilder.listenerPort(80);
        }

        ApplicationLoadBalancedFargateService service = serviceBuilder.build();

        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/q/health/ready")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(30))
                .timeout(Duration.seconds(5))
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(3)
                .build());

        service.getTaskDefinition().getTaskRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .actions(List.of(
                                "sqs:SendMessage", "sqs:GetQueueUrl", "sqs:GetQueueAttributes"))
                        .resources(List.of("*"))
                        .build());

        jwtSecret.grantRead(service.getTaskDefinition().getExecutionRole());
        dbSecret.grantRead(service.getTaskDefinition().getExecutionRole());

        Topic alarmTopic = Topic.Builder.create(this, "ApiAlarmTopic")
                .topicName("feedbacks-api-alarms")
                .displayName("Feedbacks API CloudWatch alarms")
                .build();
        Subscription.Builder.create(this, "ApiAlarmEmailSubscription")
                .topic(alarmTopic)
                .protocol(SubscriptionProtocol.EMAIL)
                .endpoint(adminEmail)
                .build();

        Alarm fiveXxAlarm = Alarm.Builder.create(this, "ApiTarget5xxAlarm")
                .alarmName("feedbacks-api-target-5xx")
                .alarmDescription("FR-14 — HTTP 5XX no target group da API (ALB); notifica SNS")
                .metric(service.getTargetGroup().getMetrics().httpCodeTarget(
                        HttpCodeTarget.TARGET_5XX_COUNT,
                        MetricOptions.builder()
                                .period(Duration.minutes(1))
                                .statistic("Sum")
                                .build()))
                .threshold(1)
                .evaluationPeriods(5)
                .datapointsToAlarm(3)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .build();
        fiveXxAlarm.addAlarmAction(new SnsAction(alarmTopic));

        String scheme = https ? "https" : "http";
        CfnOutput.Builder.create(this, "ApiAlbDns")
                .value(service.getLoadBalancer().getLoadBalancerDnsName())
                .description("DNS do ALB (Postman / health)")
                .build();
        CfnOutput.Builder.create(this, "ApiEcrRepositoryUri")
                .value(repository.getRepositoryUri())
                .description("ECR feedbacks-api (pipeline / evidência FR-15)")
                .build();
        CfnOutput.Builder.create(this, "ApiAlarmName")
                .value(fiveXxAlarm.getAlarmName())
                .description("Alarme CloudWatch FR-14")
                .build();
        CfnOutput.Builder.create(this, "ApiAlarmTopicArn")
                .value(alarmTopic.getTopicArn())
                .description("SNS do alarme 5XX — confirme o e-mail da subscription uma vez")
                .build();
        CfnOutput.Builder.create(this, "ApiEcsSecurityGroupId")
                .value(ecsSg.getSecurityGroupId())
                .description("SG das tasks ECS — autorizar no SG do RDS :5432")
                .build();
        CfnOutput.Builder.create(this, "ApiHealthUrl")
                .value(scheme
                        + "://"
                        + service.getLoadBalancer().getLoadBalancerDnsName()
                        + "/api/v1/health")
                .build();
        CfnOutput.Builder.create(this, "ApiHttpsEnabled")
                .value(https ? "true" : "false")
                .description("true se FEEDBACKS_ACM_CERT_ARN foi informado no deploy")
                .build();
    }

    /**
     * Config via env — SQS URL, secrets e certificado ACM opcional.
     */
    public record ApiStackConfig(
            String dockerContextPath,
            String ecrRepositoryName,
            String jwtSecretName,
            String dbSecretName,
            String adminEmailSecretName,
            String sqsAlertQueueUrl,
            String acmCertificateArn) {

        public static ApiStackConfig defaults() {
            String queueUrl = System.getenv("FEEDBACKS_SQS_ALERT_QUEUE_URL");
            if (queueUrl == null || queueUrl.isBlank()) {
                queueUrl =
                        "https://sqs.us-east-1.amazonaws.com/118308531450/feedbacks-avaliacao-alerta";
            }
            return new ApiStackConfig(
                    null,
                    "feedbacks-api",
                    envOr("FEEDBACKS_JWT_SECRET_NAME", "feedbacks/jwt"),
                    envOr("FEEDBACKS_DB_SECRET_NAME", "feedbacks/db"),
                    envOr("FEEDBACKS_ADMIN_EMAIL_SECRET_NAME", "feedbacks/adminEmail"),
                    queueUrl,
                    blankToNull(System.getenv("FEEDBACKS_ACM_CERT_ARN")));
        }

        private static String envOr(String key, String fallback) {
            String value = System.getenv(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
