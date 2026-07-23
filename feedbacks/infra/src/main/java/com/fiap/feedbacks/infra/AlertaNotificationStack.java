package com.fiap.feedbacks.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.constructs.Construct;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * CDK mínimo do caminho de alerta (FR-10 / AD-13 / AD-18).
 * Fora de escopo: ECS, RDS, lambda-report, EventBridge.
 */
public class AlertaNotificationStack extends Stack {

    public AlertaNotificationStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, null, null);
    }

    public AlertaNotificationStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final String functionZipPath,
            final String adminEmailSecretName) {
        super(scope, id, props);

        Queue dlq = Queue.Builder.create(this, "AvaliacaoAlertaDlq")
                .queueName("feedbacks-avaliacao-alerta-dlq")
                .retentionPeriod(Duration.days(14))
                .encryption(QueueEncryption.SQS_MANAGED)
                .build();

        Queue queue = Queue.Builder.create(this, "AvaliacaoAlertaQueue")
                .queueName("feedbacks-avaliacao-alerta")
                .visibilityTimeout(Duration.seconds(60))
                .retentionPeriod(Duration.days(4))
                .encryption(QueueEncryption.SQS_MANAGED)
                .deadLetterQueue(DeadLetterQueue.builder()
                        .queue(dlq)
                        .maxReceiveCount(3)
                        .build())
                .build();

        String secretName = adminEmailSecretName == null || adminEmailSecretName.isBlank()
                ? "feedbacks/adminEmail"
                : adminEmailSecretName;
        ISecret adminEmailSecret = Secret.fromSecretNameV2(this, "AdminEmailSecret", secretName);

        String zipPath = functionZipPath == null || functionZipPath.isBlank()
                ? Path.of("..", "apps", "notification", "target", "function.zip")
                        .normalize()
                        .toString()
                : functionZipPath;

        String adminEmail = adminEmailSecret.secretValueFromJson("adminEmail").unsafeUnwrap();

        Function notificationFn = Function.Builder.create(this, "NotificationLambda")
                .functionName("feedbacks-lambda-notification")
                .runtime(Runtime.JAVA_17)
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .code(Code.fromAsset(zipPath))
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                        "QUARKUS_LAMBDA_HANDLER", "alert",
                        "ADMIN_EMAIL", adminEmail,
                        "SES_FROM_EMAIL", adminEmail))
                .build();

        adminEmailSecret.grantRead(notificationFn);

        notificationFn.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("ses:SendEmail", "ses:SendRawEmail"))
                .resources(List.of("*"))
                .build());

        notificationFn.addEventSource(SqsEventSource.Builder.create(queue)
                .batchSize(5)
                .build());

        CfnOutput.Builder.create(this, "AlertQueueUrl")
                .value(queue.getQueueUrl())
                .description("FEEDBACKS_SQS_ALERT_QUEUE_URL para a API (%aws)")
                .build();

        CfnOutput.Builder.create(this, "NotificationFunctionName")
                .value(notificationFn.getFunctionName())
                .build();
    }
}
