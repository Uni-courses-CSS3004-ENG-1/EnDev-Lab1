package com.endev.tutoring.client;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.PaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Stands in for the payment provider until there is a real HTTP client. It only prints.
// It already receives the provider's address from yaml, so Lab 6 adds the call, not the config.
@Component
public class LoggingPaymentClient implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentClient.class);

    private final DownstreamProperties downstream;

    public LoggingPaymentClient(DownstreamProperties downstream) {
        this.downstream = downstream;
        log.info("Payment provider configured at {} with a {} timeout; no call is made yet",
                downstream.baseUrl(), downstream.timeout());
    }

    public DownstreamProperties downstream() {
        return downstream;
    }

    @Override
    public void hold(LessonId lesson) {
        System.out.println("  payment: hold for " + lesson.value());
    }

    @Override
    public void release(LessonId lesson) {
        System.out.println("  payment: release to the tutor for " + lesson.value());
    }

    @Override
    public void refund(LessonId lesson) {
        System.out.println("  payment: refund to the student for " + lesson.value());
    }
}
