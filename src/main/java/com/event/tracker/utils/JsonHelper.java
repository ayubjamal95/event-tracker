package com.event.tracker.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode object() {
        return mapper.createObjectNode();
    }

    public static ArrayNode array(String... values) {
        ArrayNode array = mapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}

