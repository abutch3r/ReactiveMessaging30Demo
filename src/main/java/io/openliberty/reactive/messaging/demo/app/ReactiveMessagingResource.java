package io.openliberty.reactive.messaging.demo.app;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/")
public class ReactiveMessagingResource {

    /* Stores messages that have been processed via the Kafka slow starting with the `rest-message-in` emitter */
    @Inject
    private MessageStore messageStore;

    @Inject
    @Channel("rest-message-in")
    Emitter<String> emitter;

    /**
     * Takes the provided name from the request and will send out via Reactive Messaging Emitter to Kafka
     *
     * Waits for the Message stream to complete before finishing.
     * @param name
     * @return
     */
    @POST
    @Path("/")
    @Operation(description = "Provide the name of a person for processing")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> receiveMessage(@Schema(description = "Name of the subject")String name){
        System.out.println("Received message "+ name);
        return emitter.send(name);
    }

    @GET
    @Operation(description = "Get all processed names")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public List<String> getMessages(){
        return messageStore.getMessages();
    }

    @Inject
    @Channel("nackMessage")
    Emitter<String> nackMessageEmitter;

    @POST
    @Operation(description = "Checks if the provided name starts with a captial letter")
    @Path("/nackMessage")
    @Consumes(MediaType.TEXT_PLAIN)
    @APIResponse(responseCode = "204", description = "Validation for the provided name passed")
    @APIResponse(responseCode = "400", description = "Validation for the provided name failed")
    public CompletionStage<Response> receiveNackableMessage(String message){
        CompletableFuture<Response> ackCf = new CompletableFuture<>();
        nackMessageEmitter.send(
            Message.of(message,
                () -> {
                    ackCf.complete(Response.status(Response.Status.NO_CONTENT).build());
                    return CompletableFuture.completedFuture(null);
                }, t -> {
                    //ackCf.completeExceptionally(t);
                    ackCf.complete(Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
                    return CompletableFuture.completedFuture(null);
                }
            )
        );
        return ackCf;
    }

    @Inject
    @Channel("nackPayload")
    Emitter<String> nackPayloadEmitter;

    @POST
    @Operation(description = "Checks if the provided name starts with a captial letter, if it does not it will be rejected")
    @Path("/nackPayload")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> receiveNackablePayload(String payload){
        return nackPayloadEmitter.send(payload);
    }

    @Inject
    @Channel("buffer")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    /**
     * Buffer has a limited capacity to encourage an overflow event
     *
     * The default size is 128 messages
     *
     * In the event of a message overflowing an exception will be thrown
     */
    Emitter<String> bufferedEmitter;

    @POST
    @Path("/buffer")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> buffer(String message){
        System.out.println("Processing buffered message " + message);
        return bufferedEmitter.send(message);
    }

    @Inject
    @Channel("drop")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    /* Emitter uses Drop strategy which means once the backend is unable to keep up, it will drop any new messages it receives
     */
    Emitter<String> dropEmitter;

    @POST
    @Operation(description = "takes in messages and attempts to process them, any messages that cannot be processed will be dropped")
    @Path("drop")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> drop(String message){
        System.out.println("Processing drop message " + message);
        return dropEmitter.send(message);
    }

    @Inject
    @Channel("latest")
    @OnOverflow(value = OnOverflow.Strategy.LATEST)
    /* Emitter uses latest strategy which means once the backend is unable to keep up, it will drop any messages that are not currently being
     * processed and only keep the latest
     *
     * e.g. if 5 messages are sent to via emitter, messages 1 and 5 would be expected to be processed, but no guarantee that 2,3 or 4 would be
     */
    Emitter<String> latestEmitter;

    @POST
    @Operation(description = "Takes in messages and attempts to process them, If the backend is unable to keep up, it will drop older messages in preference for new messages")
    @Path("latest")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> latest(String message){
        System.out.println("Processing latest message " + message);
        return latestEmitter.send(message);
    }

}
