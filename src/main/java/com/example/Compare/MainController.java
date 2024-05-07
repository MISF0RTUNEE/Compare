package com.example.Compare;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Iterator;

import static org.apache.tomcat.util.codec.binary.Base64.decodeBase64;

@RestController
public class MainController {
    private final ObjectMapper mapper = new ObjectMapper();
    @PostMapping("/delta")
    public ObjectNode compareJson(@RequestBody JsonBase64 jsonBase64) throws JsonProcessingException {
        String oldJson = decodeBase64(jsonBase64.getOldVersion());
        String newJson = decodeBase64(jsonBase64.getNewVersion());

        JsonNode oldNode = mapper.readTree(oldJson);
        JsonNode newNode = mapper.readTree(newJson);

        ObjectNode result = mapper.createObjectNode();
        compareNodes(oldNode, newNode, result);

        return result;
    }

    private void compareNodes(JsonNode oldNode, JsonNode newNode, ObjectNode result) {
        if (oldNode.equals(newNode)) {
            return;
        }

        if (oldNode.isObject() && newNode.isObject()) {
            Iterator<String> fieldNames = newNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (oldNode.has(fieldName)) {
                    JsonNode oldFieldValue = oldNode.get(fieldName);
                    JsonNode newFieldValue = newNode.get(fieldName);
                    if (oldFieldValue.isArray()) {
                        if (!oldFieldValue.equals(newFieldValue)) {
                            result.set(fieldName, newFieldValue);
                        }
                    } else {
                        ObjectNode fieldResult = mapper.createObjectNode();
                        compareNodes(oldFieldValue, newFieldValue, fieldResult);
                        if (!fieldResult.isEmpty()) {
                            result.set(fieldName, fieldResult);
                        }
                    }
                } else {
                    result.set("ins", newNode.get(fieldName));
                }
            }
            Iterator<String> oldFieldNames = oldNode.fieldNames();
            while (oldFieldNames.hasNext()) {
                String fieldName = oldFieldNames.next();
                if (!newNode.has(fieldName)) {
                    result.set("del", oldNode.get(fieldName));
                }
            }
        } else if (oldNode.isArray() && newNode.isArray()) {
            for (int i = 0; i < oldNode.size() || i < newNode.size(); i++) {
                if (i < oldNode.size() && i < newNode.size()) {
                    if (!oldNode.get(i).equals(newNode.get(i))) {
                        ObjectNode arrayResult = mapper.createObjectNode();
                        compareNodes(oldNode.get(i), newNode.get(i), arrayResult);
                        result.set(Integer.toString(i), arrayResult);
                    }
                } else if (i < oldNode.size()) {
                    result.set(Integer.toString(i), oldNode.get(i));
                } else if (i < newNode.size()) {
                    result.set(Integer.toString(i), newNode.get(i));
                }
            }
        } else {
            if (!oldNode.equals(newNode)) {
                result.put("old", oldNode.asText());
                result.put("new", newNode.asText());
            }
        }
    }

    private String decodeBase64(String base64String) {
        byte[] decodedBytes = Base64.decodeBase64(base64String);
        return new String(decodedBytes);
    }



}
