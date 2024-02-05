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

import javax.naming.InitialContext;
import javax.naming.NamingException;
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
    @Channel("propagate-all-context-out")
    Emitter<String> propagateAllEmitter;

    @Inject
    @Channel("propagate-no-context-out")
    Emitter<String> propagateNoneEmitter;

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
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> drop(String message){
        System.out.println("Processing drop message " + message);
        return dropEmitter.send(message);
    }

    @POST
    @Path("propagateAll")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> propagateAll(String message){
        System.out.println("Progagting message " + processContextMessage(message) + " with all context");
        return propagateAllEmitter.send(message);
    }

    @POST
    @Path("propagateNone")
    @Consumes(MediaType.TEXT_PLAIN)
    public CompletionStage<Void> propagateNone(String message){
        System.out.println("Progagting message " + processContextMessage(message) +" with no context");
        return propagateNoneEmitter.send(message);
    }

    public String processContextMessage(String input){
        try {
            return input + "-" + getAppName() + "-" + isTcclSet();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private String getAppName() {
        try {
            return (String) new InitialContext().lookup("java:app/AppName");
        } catch (NamingException e) {
            return "noapp";
        }
    }

    private boolean isTcclSet() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        // Check if Liberty's special classloader is the TCCL
        // Unfortunately, when the TCCL is not set, we get a context classloader which delegates based on the classes on the stack so this is the easiest way to determine whether we have a regular TCCL or not
        if (tccl.getClass().getName().endsWith("ThreadContextClassLoader")) {
            return true;
        } else {
            return false;
        }
    }

}
