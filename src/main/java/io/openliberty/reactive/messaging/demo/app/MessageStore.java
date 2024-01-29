package io.openliberty.reactive.messaging.demo.app;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class MessageStore {

    private List<String> messages;

    @PostConstruct
    private void setup() {
        messages = new ArrayList<>();
    }

    public void storeMessage(String message){
        messages.add(message);
    }

    public String getMessage(int id){
        return messages.get(id);
    }

    public List<String> getMessages(){
        return this.messages;
    }
}
