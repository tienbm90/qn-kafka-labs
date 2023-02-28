# GOAL

```
- Setup a kafka cluster
- Create a example topic
- Produce data to topic and consume data out of the topic


```

# Step 1: Get Kafka

```
Download the latest Kafka release and extract it:

$ tar -xzf kafka_2.13-3.3.1.tgz
$ cd kafka_2.13-3.3.1
```

# Step 2: Start the Kafka environment
## NOTE: Your local environment was installed Java 11.
-------
## Start Zookeeper Service

```
./bin/zookeeper-server-start.sh config/zookeeper.properties

```

## Start the Kafka broker service

```
./bin/kafka-server-start.sh config/server.properties
```

# Step 3: Create a topic to store your events

## Create topic
```
bin/kafka-topics.sh --create --topic quickstart-events --bootstrap-server localhost:9092
```

## Verify topic

```
bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092

```

# Step 4: Write some events into the topic

```
bin/kafka-console-producer.sh --topic quickstart-events --bootstrap-server localhost:9092

```

# Step 5: Read the events

```
./bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092

```