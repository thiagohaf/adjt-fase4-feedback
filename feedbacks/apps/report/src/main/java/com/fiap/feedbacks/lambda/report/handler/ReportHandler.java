package com.fiap.feedbacks.lambda.report.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fiap.feedbacks.lambda.report.application.GenerateReportUseCase;
import com.fiap.feedbacks.lambda.report.domain.Periodo;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada EventBridge / invoke manual — parse {@code periodo}; inválido falha (SPEC-12.3 / 12.5).
 */
@Named("report")
public class ReportHandler implements RequestHandler<ReportRequest, Map<String, Object>> {

    private static final Logger LOG = Logger.getLogger(ReportHandler.class);

    private final GenerateReportUseCase useCase;
    private final Instant fixedTrigger;

    @Inject
    public ReportHandler(GenerateReportUseCase useCase) {
        this(useCase, null);
    }

    /** Package-private para testes com triggerInstant fixo. */
    ReportHandler(GenerateReportUseCase useCase, Instant fixedTrigger) {
        this.useCase = useCase;
        this.fixedTrigger = fixedTrigger;
    }

    @Override
    public Map<String, Object> handleRequest(ReportRequest input, Context context) {
        if (input == null) {
            throw new IllegalArgumentException("payload obrigatório com campo periodo");
        }
        Periodo periodo = Periodo.fromWire(input.getPeriodo());
        Instant trigger = fixedTrigger != null ? fixedTrigger : Instant.now();
        LOG.infof("Invocação report periodo=%s trigger=%s", periodo.wireValue(), trigger);

        GenerateReportUseCase.ReportResult result = useCase.execute(periodo, trigger);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("periodo", periodo.wireValue());
        response.put("total", result.aggregates().total());
        response.put("mediaNota", result.aggregates().mediaNota());
        response.put("s3ObjectKey", result.s3ObjectKey());
        response.put("inicio", result.aggregates().window().inicio().toString());
        response.put("fim", result.aggregates().window().fim().toString());
        return response;
    }
}
