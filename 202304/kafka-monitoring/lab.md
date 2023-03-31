https://arctype.com/blog/kafka-tutorial-1/
https://www.confluent.io/blog/monitor-kafka-clusters-with-prometheus-grafana-and-confluent/

# 1. Technology

- Kafka
- Debezium
- Zookeeper
- Kafka Connect

[![Flow](https://arctype.com/blog/content/images/size/w1600/2021/06/kafka-diagram.png)](https://kafka.apache.org/documentation/#connect)


# 2. Use Docker to Set Up Postgres, Kafka and Debezium

We will get Kafka and Debezium up and running. By the end of this guide, you will have a project that streams events from a table to a Kafka topic.

We'll be using Docker and Docker Compose to help us get Postgres, Kafka, and Debezium set up. If you aren't familiar with those tools, it may be helpful to read up on them before continuing.

## Create a Postgres Container with Docker

First, let's get a basic Postgres container set up.

```Dockerfile
version: '3.9'
services:
  db:
    image: postgres:13
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_PASSWORD=bigdata

```
After running docker-compose up, we should have a functioning Postgres database.

```bash
db_1  | 2021-05-22 03:03:59.860 UTC [47] LOG:  database system is ready to accept connections

```

Now, let's verify it's working.
```
$ psql -h 127.0.0.1 -U postgres
Password for user postgres:

127 postgres@postgres=# 

```

## Add Debezium's Kafka, Kafka Connect and ZooKeeper Images

Now let's add the other images we'll need for Kafka. Debezium happens to offer images of Kafka, Kafka Connect, and ZooKeeper that are designed specifically to work with Debezium. So we'll go ahead and use their images.

```
version: '3.9'
services:
  db:
    image: postgres:13
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_PASSWORD=bigdata

  zookeeper:
    image: debezium/zookeeper
    ports:
      - "2181:2181"
      - "2888:2888"
      - "3888:3888"

  kafka:
    image: debezium/kafka
    ports:
      - "9092:9092"
      - "29092:29092"
    depends_on:
      - zookeeper
    environment:
      - ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=LISTENER_EXT://localhost:29092,LISTENER_INT://kafka:9092
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=LISTENER_INT:PLAINTEXT,LISTENER_EXT:PLAINTEXT
      - KAFKA_LISTENERS=LISTENER_INT://0.0.0.0:9092,LISTENER_EXT://0.0.0.0:29092
      - KAFKA_INTER_BROKER_LISTENER_NAME=LISTENER_INT

  connect:
    image: debezium/connect:1.6
    ports:
      - "8083:8083"
    environment:
      - BOOTSTRAP_SERVERS=kafka:9092
      - GROUP_ID=1
      - CONFIG_STORAGE_TOPIC=my_connect_configs
      - OFFSET_STORAGE_TOPIC=my_connect_offsets
      - STATUS_STORAGE_TOPIC=my_connect_statuses
      - KAFKA_AUTO_CREATE_TOPIS_ENABLE='true'

    depends_on:
      - zookeeper
      - kafka

```

The environment variables for the Kafka setup let you set up different network and security protocols if your network setup has different rules for intra-broker communication vs. external clients connecting to Kafka. Our simple setup is all self-contained within the network Docker creates for us.

Kafka Connect creates topics in Kafka and uses them to store configurations. You can specify the name it will use for the topics with environment variables. If you have multiple Kafka Connect nodes, they can parallelize their workload when they have the same GROUP_ID and *_STORAGE_TOPIC configurations. More details on Connect configuration are available here.

## Streaming Events to PostgreSQL
Let's create a table to test event streaming.

```
create table test (
id serial primary key,
name varchar
);

```

## Set Up A Debezium Connector for PostgreSQL

If we start our Docker project, Kafka, Kafka Connect, ZooKeeper, and Postgres will run just fine. However, Debezium requires us to explicitly set up a connector to start streaming data from Postgres.

Before we activate Debezium, we need to prepare Postgres by making some configuration changes. Debezium utilizes something built into Postgres called a WAL, or write-ahead log. Postgres uses this log to ensure data integrity and manage row versions and transactions. Postgres' WAL has several modes you can configure it to, and for Debezium to work, the WAL level must be set to replica. Let's change that now.

```
psql> alter system set wal_level to 'replica';

```

You may need to restart the Postgres container for this change to take effect.

There is one Postgres plugin not included with the image we used that we will need: wal2json. Debezium can work with either wal2json or protobuf. For this tutorial, we will use wal2json. As its name implies, it converts Postgres' write-ahead logs to JSON format.

With our Docker app running, let's manually install wal2json using aptitude. To get to the shell of the Postgres container, first find the container ID and then run the following command to open bash:

```
$ docker ps

CONTAINER ID   IMAGE               
c429f6d35017   debezium/connect    
7d908378d1cf   debezium/kafka      
cc3b1f05e552   debezium/zookeeper  
4a10f43aad19   postgres:latest     

$ docker exec -ti 4a10f43aad19 bash
```

Now that we're inside the container, let's install wal2json:

```
$ apt-get update && apt-get install postgresql-13-wal2json

```

## Activate Debezium

We're ready to activate Debezium!

We can communicate with Debezium by making HTTP requests to it. We need to make a POST request whose data is a configuration in JSON format. This JSON defines the parameters of the connector we're attempting to create. We'll put the configuration JSON into a file and then use cURL to send it to Debezium.

You have several configuration options at this point. This is where you can use a whitelist or blacklist if you only want Debezium to stream certain tables (or avoid certain tables).

```
$ echo '
{
    "name": "arctype-connector",
    "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "tasks.max": "1",
        "plugin.name": "wal2json",
        "database.hostname": "db",
        "database.port": "5432",
        "database.user": "postgres",
        "database.password": "bigdata",
        "database.dbname": "postgres",
        "database.server.name": "QN",
        "key.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "key.converter.schemas.enable": "false",
        "value.converter.schemas.enable": "false",
        "snapshot.mode": "always"
    }
}
' > debezium.json

```

Now we can send this configuration to Debezium.

```
$ curl -i -X POST \
         -H "Accept:application/json" \
         -H "Content-Type:application/json" \
         127.0.0.1:8083/connectors/ \
         --data "@debezium.json"
```

The response will be a JSON representation of the newly initiated connector.

```
{
  "name": "arctype-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "plugin.name": "wal2json",
    "database.hostname": "db",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "arctype",
    "database.dbname": "postgres",
    "database.server.name": "ARCTYPE",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",
    "snapshot.mode": "always",
    "name": "arctype-connector"
  },
  "tasks": [],
  "type": "source"
}
```

## Test Kafka Streaming Setup

Now we are streaming! After inserting, updating, or deleting a record, we will see the changes as a new message in the Kafka topic associated with the table. Kafka Connect will create 1 topic per SQL table. To verify that this is working correctly, we'll need to monitor the Kafka topic.

Kafka comes with some shell scripts that help you poke around your Kafka configuration. They are handy when you want to test your configuration and are conveniently included in the Docker image we are using. The first one we'll use lists all of the topics in your Kafka cluster. Let's run it and verify that we see a topic for our `test` table.

```
$ docker exec -it \
  $(docker ps | grep arctype-kafka_kafka | awk '{ print $1 }') \
  /kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --list
    
ARCTYPE.public.test
__consumer_offsets
my_connect_configs
my_connect_offsets
my_connect_statuses
```

docker exec allows you to execute a command inside of a container without having to enter into its shell. Docker requires you to specify the container id when you use docker exec. When you re-create docker containers, the ID will change, making it futile to memorize that ID. $(docker ps | grep arctype-kafka_kafka | awk '{ print $1 }') finds the correct container ID by listing all active docker containers (docker ps), running them through grep to find the one that is running pure Kafka and then using awk to cherry-pick just the first column of the output, which will be the container id. the $() syntax runs a command and inserts its output in place.

The built-in Kafka tools require you to specify --bootstrap-server. They refer to it as bootstrap because you'll usually run Kafka as a cluster with several nodes, and you need one of them that is public-facing for your consumer to "enter the mix." Kafka handles the rest on its own.

You can see our test table is listed as ARCTYPE.public.test. The first part, QN, is a prefix that we set with the database.server.name field in the JSON configuration. The second part represents which Postgres schema the table is in, and the last part is the table name. Once you write more Kafka producers and stream applications, you'll have many more topics, so it's helpful to set the prefix to make it easy to identify which topics are pure SQL tables.


Now we can use another tool called the console consumer to watch the topic in real-time. It's called "console consumer" because it is a type of Kafka "Consumer"— a utility that consumes messages from a topic and does something with them. A consumer can do anything with the data it ingests, and the console consumer does nothing besides print it out to the console.

```
$ docker exec -it \
  $(docker ps | grep arctype-kafka_kafka | awk '{ print $1 }') \
  /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic ARCTYPE.public.test

```
By default, the console consumer only consumes messages it hasn't already. If you want to see every message in a topic, you can add --from-beginning to the console command.

Now that our consumer is watching the topic for new messages, we run an INSERT and watch for output.
```
postgres=# insert into test (name) values ('QN Kafka Test!');
INSERT 0 1
```

Back on our Kafka consumer:
```
$ docker exec -it $(docker ps | grep arctype-kafka_kafka | awk '{ print $1 }') /kafka/bin/kafka-console-consumer.sh  --bootstrap-server localhost:9092 --topic ARCTYPE.public.test
...
{
  "before": null,
  "after": {
    "id": 8,
    "name": "Arctype Kafka Test!"
  },
  "source": {
    "version": "1.5.0.Final",
    "connector": "postgresql",
    "name": "ARCTYPE",
    "ts_ms": 1621913280954,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22995096\",\"22995096\"]",
    "schema": "public",
    "table": "test",
    "txId": 500,
    "lsn": 22995288,
    "xmin": null
  },
  "op": "c",
  "ts_ms": 1621913280982,
  "transaction": null
} 
```

Along with some metadata, you can see the primary key and the `name` field of the record we inserted!


