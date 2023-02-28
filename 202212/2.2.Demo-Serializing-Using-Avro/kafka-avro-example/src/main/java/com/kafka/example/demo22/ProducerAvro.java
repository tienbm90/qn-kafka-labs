package com.kafka.example.demo22;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

public class ProducerAvro {

    public static void main(final String[] args) throws IOException {
        String configPath = "";
        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
//            System.exit(1);
            configPath = "getting-started.properties";
        }else {
            configPath = args[0];
        }

        // Load producer configuration settings from a local file
        final Properties props = loadConfig(configPath);
        final String topic = "purchases";

        try (final Producer<String, byte[]> producer = new KafkaProducer<>(props)) {
            Schema schema1= new Schema.Parser().parse("{\"namespace\":\"student.avro \",\"type\":\"record\",\"name\":\"Student\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"}]}");

            System.out.println(schema1.getName());
            final Long numMessages = 10L;
            for (Long i = 0L; i < numMessages; i++) {

                // Create avro generic record object
                GenericRecord genericUser = new GenericData.Record(schema1);
                //Put data in that generic record
                genericUser.put("name", "John Wick");
                genericUser.put("age", 1234 + i);


                SpecificDatumWriter<GenericRecord> datumWriter = new SpecificDatumWriter<>(schema1);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.reset();

                BinaryEncoder binaryEncoder = new EncoderFactory().binaryEncoder(byteArrayOutputStream,null);

                datumWriter.write(genericUser, binaryEncoder);
                binaryEncoder.flush();
                producer.send(
                        new ProducerRecord<String, byte[]>(topic, byteArrayOutputStream.toByteArray()),
                        (event, ex) -> {
                            if (ex != null)
                                ex.printStackTrace();
                            else
                                System.out.printf("Produced event to topic %s: key = %-10s value = %s%n", topic, genericUser.get("name"), genericUser.get("age"));
                        });
            }
            System.out.printf("%s events were produced to topic %s%n", numMessages, topic);
        }

    }

    // We'll reuse this function to load properties from the Consumer as well
    public static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }

}