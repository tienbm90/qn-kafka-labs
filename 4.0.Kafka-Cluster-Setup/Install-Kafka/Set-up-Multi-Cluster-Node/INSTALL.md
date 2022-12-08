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


## Start the first Kafka broker service

```
./bin/kafka-server-start.sh config/server.properties
```

## Configuration the second Kafka broker

### Create new configuration file for new broker

```
cp config/server.properties  config/server2.properties
```

### Update new configurations

Open server2.properties and edit following configurations

```
broker.id=1
log.dirs=/tmp/kafka-logs2
listeners=PLAINTEXT://:9093 

```

### Start new broker

```
./bin/kafka-server-start.sh config/server2.properties

```


# Step 3: Verify installation

1. Visit https://zookeeper.apache.org/releases.html to download Zookeeper version 3.6.3

2. Copy to VM and place in /data/lab

3. Extract 

```

$ cd /data/lab
$ tar -xzf apache-zookeeper-3.6.3-bin.tar.gz
$ cd apache-zookeeper-3.6.3-bin/
```

4. Using Zookeeper Cli to connect to Zookeeper

```
./bin/zkCli.sh -server localhost:2181


ls /brokers/ids/

get /broker/ids/1


```

# Step 4: Create a topic to store your events

## Create topic
```
cd /data/lab/kafka_2.12-3.3.1/
bin/kafka-topics.sh --create --topic events --bootstrap-server localhost:9092
```

## Verify topic

```
bin/kafka-topics.sh --describe --topic events --bootstrap-server localhost:9092

```

# Step 5: Write some events into the topic

```
bin/kafka-console-producer.sh --topic events --bootstrap-server localhost:9092

```

# Step 6: Read the events

```
./bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092

```