package com.flightmonitor.unit.messaging;

import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.sync.SyncMessageBus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"sync", "test"})
class MessageBusFactoryTest {

    @Autowired
    private MessageBus messageBus;

    @Test
    void syncProfile_loadsSyncMessageBus() {
        assertThat(messageBus).isInstanceOf(SyncMessageBus.class);
        assertThat(messageBus.getBrokerType()).isEqualTo(BrokerType.SYNC);
        assertThat(messageBus.isAvailable()).isTrue();
    }
}
