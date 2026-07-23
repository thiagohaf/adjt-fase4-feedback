package com.fiap.feedbacks.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.cxapi.CloudAssembly;

/**
 * Entry point CDK Java — alerta (FR-10) + relatório (FR-11/12/17).
 */
public final class FeedbacksAlertaApp {

    private FeedbacksAlertaApp() {
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder()
                .outdir("cdk.out")
                .build());

        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv("CDK_DEFAULT_REGION");
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }

        Environment env = Environment.builder()
                .account(account)
                .region(region)
                .build();

        new AlertaNotificationStack(
                app,
                "FeedbacksAlertaNotificationStack",
                StackProps.builder()
                        .env(env)
                        .description("FR-10 — SQS + DLQ + lambda-notification (SES); sem ECS/RDS/report")
                        .build());

        new RelatorioStack(
                app,
                "FeedbacksRelatorioStack",
                StackProps.builder()
                        .env(env)
                        .description(
                                "FR-11/12/17 — EventBridge + lambda-report (VPC) + S3 + SES; sem ECS/ECR")
                        .build());

        CloudAssembly assembly = app.synth();
        System.out.println("CDK synth OK — " + assembly.getStacks().size() + " stack(s) em "
                + assembly.getDirectory());
    }
}
