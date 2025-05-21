package com.example.customerservice.service;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdModule extends SimpleModule {
    public ObjectIdModule() {
        addSerializer(ObjectId.class, new JsonSerializer<ObjectId>() {
            @Override
            public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.toHexString());
            }
        });

        addDeserializer(ObjectId.class, new JsonDeserializer<ObjectId>() {
            @Override
            public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, IOException {
                return new ObjectId(p.getValueAsString());
            }
        });
    }
}
