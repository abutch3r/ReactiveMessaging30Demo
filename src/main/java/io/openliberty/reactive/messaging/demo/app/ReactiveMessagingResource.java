package io.openliberty.reactive.messaging.demo.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Path("/")
@ApplicationScoped
public class ReactiveMessagingResource {

    @Inject
    @Channel("message-in")
    Emitter emitter;

    @Inject
    private MessageStore messageStore;

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
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

}
