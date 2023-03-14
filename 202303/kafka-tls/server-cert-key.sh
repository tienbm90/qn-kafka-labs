#! /bin/bash


# Create server key and certificate
openssl genrsa -out server.key

openssl req -new -key server.key -out server_reqout.txt

openssl x509 -req -in server_reqout.txt -days 3650 -sha256 -CAcreateserial -CA root.crt \
-CAkey root.key -out server.crt

# 
openssl genrsa -out client.key

openssl req -new -key client.key -out client_reqout.txt

openssl x509 -req -in client_reqout.txt -days 3650 -sha256 -CAcreateserial -CA root.crt \
  -CAkey root.key -out client.crt



