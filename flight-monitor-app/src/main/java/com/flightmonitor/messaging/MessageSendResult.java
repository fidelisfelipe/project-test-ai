package com.flightmonitor.messaging;

public record MessageSendResult(
        boolean success,
        String brokerType,
        String messageId,
        long processingTimeMs,
        String errorMessage
) {
    public static MessageSendResult ok(BrokerType brokerType, String messageId, long processingTimeMs) {
        return new MessageSendResult(true, brokerType.name(), messageId, processingTimeMs, null);
    }

    public static MessageSendResult failed(BrokerType brokerType, String errorMessage) {
        return new MessageSendResult(false, brokerType.name(), null, 0, errorMessage);
    }
}
