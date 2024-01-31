package io.openliberty.reactive.messaging.demo.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class MessagingBean {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Throwable downstreamFailure;

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
    @Outgoing("bufferOut")
    public PublisherBuilder<String> handleBufferedMessages(final PublisherBuilder<String> values){
        return values
                .via(ReactiveStreams.<String>builder().flatMapCompletionStage(s -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return s;
                }, executor))).onError(err -> downstreamFailure = err);
    }

    @Incoming("bufferOut")
    public void bufferOut(String message){
        System.out.println("Completed processing of message: "+ message);
    }

    @Incoming("drop")
    @Outgoing("dropOut")
    public PublisherBuilder<String> handleDroppableMessages(final PublisherBuilder<String> values){
        return values
                .via(ReactiveStreams.<String>builder().flatMapCompletionStage(s -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return s;
                }, executor))).onError(err -> downstreamFailure = err);
    }

    @Incoming("dropOut")
    public void dropOut(String message){
        System.out.println("didn't drop message "+ message);
    }
}
