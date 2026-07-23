package com.fiap.feedbacks.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.cxapi.CloudAssembly;

/**
 * Entry point CDK Java — stack mínimo de alerta (FR-10).
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

        new AlertaNotificationStack(
                app,
                "FeedbacksAlertaNotificationStack",
                StackProps.builder()
                        .env(Environment.builder()
                                .account(account)
                                .region(region)
                                .build())
                        .description("FR-10 — SQS + DLQ + lambda-notification (SES); sem ECS/RDS/report")
                        .build());

        CloudAssembly assembly = app.synth();
        System.out.println("CDK synth OK — " + assembly.getStacks().size() + " stack(s) em "
                + assembly.getDirectory());
    }
}
