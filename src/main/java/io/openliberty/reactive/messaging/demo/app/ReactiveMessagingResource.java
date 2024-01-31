package io.openliberty.reactive.messaging.demo.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/")
public class ReactiveMessagingResource {

    @Inject
    @Channel("rest-message-in")
    Emitter<String> emitter;

    @Inject
    @Channel("buffer")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    Emitter<String> bufferedEmitter;

    @Inject
    @Channel("drop")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<String> dropEmitter;

    @Inject
    @Channel("nack")
    Emitter<String> nackEmitter;

    @Inject
    private MessageStore messageStore;

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> receiveMessage(String message){
        System.out.println("Received message "+ message);
        return emitter.send(message);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public List<String> getMessages(){
        return messageStore.getMessages();
    }

    @POST
    @Path("/nack")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Response>  receiveNackableMessage(String message){
        CompletableFuture<Response> ackCf = new CompletableFuture<>();
        nackEmitter.send(
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

    @POST
    @Path("/buffer")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> buffer(String message){
        System.out.println("Processing buffered message " + message);
        return bufferedEmitter.send(message);
    }

    @POST
    @Path("drop")
    public CompletionStage<Void> drop(String message){
        System.out.println("Processing drop message " + message);
        return dropEmitter.send(message);
    }

}
