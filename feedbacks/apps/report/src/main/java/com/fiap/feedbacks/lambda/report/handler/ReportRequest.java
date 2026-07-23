package com.fiap.feedbacks.lambda.report.handler;

/**
 * Payload JSON da Lambda: {@code { "periodo": "diario" | "semanal" }} (AD-6 / AD-10).
 */
public class ReportRequest {

    private String periodo;

    public ReportRequest() {
    }

    public ReportRequest(String periodo) {
        this.periodo = periodo;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }
}
