package io.openliberty.reactive.messaging.demo.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class MessagingBean {

    @Inject
    MessageStore messageStore;

    @Incoming("kafka-message-in")
    @Outgoing("kafka-message-out")
    public String processMessage(String message){
        return "hello " + message;
    }

    @Incoming("kafka-message-return")
    public void storeMessage(String message){
        messageStore.storeMessage(message);
    }
}
