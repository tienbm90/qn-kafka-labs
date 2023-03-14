#! /bin/bash

#https://www.vertica.com/docs/9.3.x/HTML/Content/Authoring/KafkaIntegrationGuide/TLS-SSL/KafkaTLS-SSLExamplePart2ConfigureVertica.htm?tocpath=Integrating%20with%20Apache%20Kafka%7CUsing%20TLS%252FSSL%20Encryption%20with%20Kafka%7C_____6

openssl genrsa -out root.key

openssl req -new -x509 -key root.key -out root.crt



