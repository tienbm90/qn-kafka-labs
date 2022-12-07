package com.kafka.java.demo21;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StudentSerializer implements Serializer<Student> {
    private final ObjectMapper objectMapper = new ObjectMapper();;
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
//        Serializer.super.configure(configs, isKey);
//         final ObjectMapper objectMapper =
    }

    @Override
    public byte[] serialize(String topic, Student data) {
        try {
            if (data == null){
                System.out.println("Null received at serializing");
                return null;
            }
            System.out.println("Serializing...");
            System.out.println(data.Name());
//            return objectMapper.writeValueAsBytes(data);
            return (data.Name() + "," + data.Age()).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing StudentSerializer to byte[]");
        }
    }

//    @Override
//    public byte[] serialize(String topic, Headers headers, Student data) {
//        return Serializer.super.serialize(topic, headers, data);
//    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}
