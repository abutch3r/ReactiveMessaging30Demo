package io.openliberty.reactive.messaging.demo.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class MessagingBean {

    @Inject
    MessageStore messageStore;

    @Incoming("kafka-message-in")
    @Outgoing("kafka-message-out")
    public String processMessage(String name){
        System.out.println("Saying hello to " + name);
        return "hello " + name;
    }

    @Incoming("kafka-message-return")
    public void storeMessage(String message){
        System.out.println("storing " + message);
        messageStore.storeMessage(message);
    }

    @Incoming("nack")
    public CompletionStage<Void> nackMessage(Message message){
        if (message.getPayload().equals("alex")){
            return message.nack(new Exception("I don't like alex"));
        } else {
            return message.ack();
        }
    }

    @Incoming("buffer")
    public CompletionStage<Void> handleBufferedMessages(Message message){
        System.out.println("Received buffered message " + message.getPayload());
        try {
            Thread.sleep(5000);
            return message.ack();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
