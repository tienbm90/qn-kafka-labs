# Start kafka cluster 

```
docker-compose up -d

```

# Register a schema

```
{
  "type": "record",
  "name": "Payment",
  "namespace": "io.confluent.examples.clients.basicavro",
  "fields": [
    {
      "name": "id",
      "type": "string"
    },
    {
      "name": "amount",
      "type": "double"
    }
  ]
}

```

Run this command:

```
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"io.confluent.examples.clients.basicavro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"}]}"}' \
  http://localhost:8081/subjects/test-value/versions

```

# View all the subject registered in Schema Registry

```
curl --silent -X GET http://localhost:8081/subjects

```

# To view the latest schema for this subject in more detail

```
curl --silent -X GET http://localhost:8081/subjects/test-value/versions/latest

```


# Update new version of schema

```
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"io.confluent.examples.clients.basicavro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"},\"age\":\"int\"]}"}' \
  http://localhost:8081/subjects/test-value/versions

```


# List out all version of schema

```

curl --silent -X GET http://localhost:8081/subjects/test-value/versions

```