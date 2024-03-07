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

    /**
     * Takes a messages of of the kafka topic for the kafka-message-in channel
     * Once processed returns the message to be stored on the kafka topic backing the kafka-message-out channel
     */
    @Incoming("kafka-message-in")
    @Outgoing("kafka-message-out")
    public String processMessage(String name){
        System.out.println("Saying hello to " + name);
        return "hello " + name;
    }

    /**
     * Stores message from received from the kafka-message-return channel in the internal store
     */
    @Incoming("kafka-message-return")
    public void storeMessage(String message){
        System.out.println("Storing " + message);
        messageStore.storeMessage(message);
    }

    /**
     * Checks names provided start with a capital, if they do not they are rejected with an IllegalArgumentException
     *
     * As the method recevies an Object of type Message, it must be manually acked or nacked. if the paylaod was being processed
     * the framework would be able to han
     */
    @Incoming("nackMessage")
    public CompletionStage<Void> nackMessage(Message message){
        if (!Character.isUpperCase(message.getPayload().toString().charAt(0))){
            return message.nack(new IllegalArgumentException("Name does not start with a capital letter"));
        } else {
            return message.ack();
        }
    }

    @Incoming("nackPayload")
    public void nackPayload(String payload){
        if (!Character.isUpperCase(payload.charAt(0))){
            throw new IllegalArgumentException("Name does not start with a capital letter");
        }
    }

    /**
     * Handles messages from the `buffer` outgoing channel emitter.
     *
     * Each message is held for one second before processing, creating the impression of processing of the the message
     * It is expected that under load this will cause messages to no longer be buffered
     *
     * If the message is processed it is returned to the `bufferOut` channel for completing processing
     */
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
    /**
     * If a message reaches this point, it is acknowledged via the framework
     */
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
        System.out.println("Did not drop message "+ message);
    }

    @Incoming("latest")
    @Outgoing("latestOut")
    public PublisherBuilder<String> handleLatestMessages(final PublisherBuilder<String> values){
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

    @Incoming("latestOut")
    public void latestOut(String message){
        System.out.println("Did not drop message "+ message);
    }
}
