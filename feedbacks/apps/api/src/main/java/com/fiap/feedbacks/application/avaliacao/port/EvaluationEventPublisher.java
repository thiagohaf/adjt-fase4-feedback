package com.fiap.feedbacks.application.avaliacao.port;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;

public interface EvaluationEventPublisher {

    void publish(AvaliacaoAlertaEvent event);
}
