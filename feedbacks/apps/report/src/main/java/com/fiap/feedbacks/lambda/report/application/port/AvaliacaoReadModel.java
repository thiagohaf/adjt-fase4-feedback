package com.fiap.feedbacks.lambda.report.application.port;

import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;

import java.util.List;

/**
 * Read model Avaliações — somente leitura (SPEC-11.3 / AD-16).
 */
public interface AvaliacaoReadModel {

    List<AvaliacaoSnapshot> findInWindow(ReportWindow window);
}
