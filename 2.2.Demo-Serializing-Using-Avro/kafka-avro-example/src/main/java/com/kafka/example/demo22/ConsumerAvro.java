package com.kafka.example.demo22;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerAvro {
    public static void main(String[] args) throws IOException {
        String configPath = "";
        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
//            System.exit(1);
            configPath = "getting-started.properties";
        }else {
            configPath = args[0];
        }


        final String topic = "purchases";

        // Load consumer configuration settings from a local file
        // Reusing the loadConfig method from the ProducerExample class
        final Properties props = ProducerAvro.loadConfig(configPath);

        // Add additional properties.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-java-demo22");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
         Schema schema1= new Schema.Parser().parse("{\"namespace\":\"student.avro \",\"type\":\"record\",\"name\":\"Student\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"}]}");

        try (final Consumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Arrays.asList(topic));
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    String key = record.key();
                    System.out.println(key);
                    DatumReader datumReader= new SpecificDatumReader(schema1);
                    Decoder decoder = DecoderFactory.get().binaryDecoder(record.value(), null);
//                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
                    GenericRecord genericRecord = (GenericRecord) datumReader.read(null, decoder);
                    System.out.println(
                            String.format("Consumed event from topic %s: key = %-10s value = %s", topic, key, genericRecord.get("name")));
                }
            }
        }
    }
}

