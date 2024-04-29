package io.knav.pgjobqueue.advancedscheduler.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public record Metadata(String documentId) {

    public String asJson() {
        var gson = new Gson();
        return gson.toJson(this);
    }

    public static Metadata fromJson(String json) {

        var gson = new Gson();
        return gson.fromJson(json, Metadata.class);
    }
}
