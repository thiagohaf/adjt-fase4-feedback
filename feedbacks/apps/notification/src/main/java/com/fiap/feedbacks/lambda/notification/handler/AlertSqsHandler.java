package com.fiap.feedbacks.lambda.notification.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fiap.feedbacks.lambda.notification.application.ProcessAlertUseCase;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

/**
 * Entrada SQS da Lambda — delega ao use case; falhas propagam para retry/DLQ (SPEC-10.4/10.5).
 */
@Named("alert")
public class AlertSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger LOG = Logger.getLogger(AlertSqsHandler.class);

    private final ProcessAlertUseCase processAlertUseCase;

    @Inject
    public AlertSqsHandler(ProcessAlertUseCase processAlertUseCase) {
        this.processAlertUseCase = processAlertUseCase;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            LOG.warn("SQSEvent sem records — nada a processar");
            return null;
        }
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            String messageId = record.getMessageId();
            try {
                processAlertUseCase.process(record.getBody());
            } catch (RuntimeException ex) {
                LOG.errorf(ex, "Falha ao processar mensagem SQS messageId=%s — retry/DLQ", messageId);
                throw ex;
            }
        }
        return null;
    }
}
