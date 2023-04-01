# Introduction
In this article, we are going to discuss how to set up Kafka monitoring using Prometheus. Kafka is one of the most widely used streaming platforms, and Prometheus is a popular way to monitor Kafka. We will use Prometheus to pull metrics from Kafka and then visualize the important metrics on a Grafana dashboard.

# Introduction to Kafka
Kafka is the most widely used streaming technology built by Confluent. What makes Kafka good is its very seamless high availability and scalability. At the same time, the out-of-the-box installation of Kafka comes with very basic command line monitoring tools. It is very important to monitor the health of Kafka in production deployments so that if your Kafka is trending in a negative direction, then you can catch the issues before it suddenly falls over. 

 ‍
# Kafka Architecture
Kafka is a distributed streaming platform. Primarily, Kafka's architecture consists of:

Topics - A topic is a logical entity on which records are published. A topic consists of the number of partitions. Each record within a topic is assigned a partition along with incrementing offset. 
Producers - Producers publish data on the topic. Producers can either provide the partition number to which the record is to be published or a hash key that Kafka will consistently use to distribute the data to multiple topics.
Consumers - Consumers read data from the topic. Consumers can be distributed across multiple machines. Each consumer is identified with a consumer group. 

# Introduction to Prometheus
Prometheus is an open-source alerting and monitoring tool developed by SoundCloud in 2012. We are not going to explain the basics of Prometheus in this article in detail. For introductions to Prometheus, please refer to our articles below:

Prometheus Monitoring 101

Monitoring a Python web app with Prometheus

Monitoring Kubernetes tutorial: using Grafana and Prometheus

# Setting up Kafka monitoring using Prometheus
We will use docker to set up a test environment of Kafka, Zookeeper, Prometheus, and Grafana. We will use the docker images available at:

https://hub.docker.com/r/grafana/grafana/

https://hub.docker.com/r/prom/prometheus/

https://hub.docker.com/r/wurstmeister/kafka/

For tutorials on how to set up Prometheus and Grafana with Docker, check out our articles on How to Deploy Prometheus on Kubernetes, and Connecting Prometheus and Grafana where both articles show different methods to set up a test environment with Docker. 

# Using JMX exporter to expose JMX metrics
Java Management Extensions (JMX) is a technology that provides the tools for providing monitoring within applications built on JVM. 

Since Kafka is written in Java, it extensively uses JMX technology to expose its internal metrics over the JMX platform.

JMX Exporter is a collector that can run as a part of an existing Java application (such as Kafka) and expose its JMX metrics over an HTTP endpoint, which can be consumed by any system such as Prometheus. For more information about Prometheus exporters, here is our article that deep dives into how Prometheus exporters work.

# Setting up the Dockerfile, configuring Prometheus.yml, and running the instances
As a first step, we will build a Kafka docker image, which will include the JMX exporter instance running as part of the Kafka instance.

## Create folder to store dependencies to enable Kafka JMX
```
mkdir ./kafka-libs

touch jmx-agent-config.yml
```


The configuration file jmx-agent-config.yml is available here:

```
lowercaseOutputName: true
rules:
- pattern : kafka.cluster<type=(.+), name=(.+), topic=(.+), partition=(.+)><>Value
  name: kafka_cluster_$1_$2
  labels:
    topic: "$3"
    partition: "$4"
- pattern : kafka.log<type=Log, name=(.+), topic=(.+), partition=(.+)><>Value
  name: kafka_log_$1
  labels:
    topic: "$2"
    partition: "$3"
- pattern : kafka.controller<type=(.+), name=(.+)><>(Count|Value)
  name: kafka_controller_$1_$2
- pattern : kafka.network<type=(.+), name=(.+)><>Value
  name: kafka_network_$1_$2
- pattern : kafka.network<type=(.+), name=(.+)PerSec, request=(.+)><>Count
  name: kafka_network_$1_$2_total
  labels:
    request: "$3"
- pattern : kafka.network<type=(.+), name=(\w+), networkProcessor=(.+)><>Count
  name: kafka_network_$1_$2
  labels:
    request: "$3"
  type: COUNTER
- pattern : kafka.network<type=(.+), name=(\w+), request=(\w+)><>Count
  name: kafka_network_$1_$2
  labels:
    request: "$3"
- pattern : kafka.network<type=(.+), name=(\w+)><>Count
  name: kafka_network_$1_$2
- pattern : kafka.server<type=(.+), name=(.+)PerSec\w*, topic=(.+)><>Count
  name: kafka_server_$1_$2_total
  labels:
    topic: "$3"
- pattern : kafka.server<type=(.+), name=(.+)PerSec\w*><>Count
  name: kafka_server_$1_$2_total
  type: COUNTER

- pattern : kafka.server<type=(.+), name=(.+), clientId=(.+), topic=(.+), partition=(.*)><>(Count|Value)
  name: kafka_server_$1_$2
  labels:
    clientId: "$3"
    topic: "$4"
    partition: "$5"
- pattern : kafka.server<type=(.+), name=(.+), topic=(.+), partition=(.*)><>(Count|Value)
  name: kafka_server_$1_$2
  labels:
    topic: "$3"
    partition: "$4"
- pattern : kafka.server<type=(.+), name=(.+), topic=(.+)><>(Count|Value)
  name: kafka_server_$1_$2
  labels:
    topic: "$3"
  type: COUNTER

- pattern : kafka.server<type=(.+), name=(.+), clientId=(.+), brokerHost=(.+), brokerPort=(.+)><>(Count|Value)
  name: kafka_server_$1_$2
  labels:
    clientId: "$3"
    broker: "$4:$5"
- pattern : kafka.server<type=(.+), name=(.+), clientId=(.+)><>(Count|Value)
  name: kafka_server_$1_$2
  labels:
    clientId: "$3"
- pattern : kafka.server<type=(.+), name=(.+)><>(Count|Value)
  name: kafka_server_$1_$2

- pattern : kafka.(\w+)<type=(.+), name=(.+)PerSec\w*><>Count
  name: kafka_$1_$2_$3_total
- pattern : kafka.(\w+)<type=(.+), name=(.+)PerSec\w*, topic=(.+)><>Count
  name: kafka_$1_$2_$3_total
  labels:
    topic: "$4"
  type: COUNTER
- pattern : kafka.(\w+)<type=(.+), name=(.+)PerSec\w*, topic=(.+), partition=(.+)><>Count
  name: kafka_$1_$2_$3_total
  labels:
    topic: "$4"
    partition: "$5"
  type: COUNTER
- pattern : kafka.(\w+)<type=(.+), name=(.+)><>(Count|Value)
  name: kafka_$1_$2_$3_$4
  type: COUNTER
- pattern : kafka.(\w+)<type=(.+), name=(.+), (\w+)=(.+)><>(Count|Value)
  name: kafka_$1_$2_$3_$6
  labels:
    "$4": "$5"
```

Download Jmx-exporter library from https://github.com/prometheus/jmx_exporter then place it into kafka-libs directory

```
curl -XGET https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.18.0/jmx_prometheus_javaagent-0.18.0.jar  -output jmx_prometheus_javaagent-0.18.0.jar

```

Verify our preparation:
```
% ls kafka-libs 
jmx-agent-config.yml                    jmx_prometheus_javaagent-0.18.0.jar
```

Once we have the above file as Dockerfile, we can create our docker-compose.yml which would contain configurations for each of our services: Prometheus, Grafana, Zookeeper, and Kafka.


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
    environment:
      - KAFKA_JMX_PORT=9101
      - ZOOKEEPER_CLIENT_PORT=2181
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181

  kafka:
    image: debezium/kafka
    container_name: kafka
    ports:
      - "9092:9092"
      - "29092:29092"
      - "9101:9101"
      - "7071:7071"
    depends_on:
      - zookeeper
    environment:
      - ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=LISTENER_EXT://localhost:29092,LISTENER_INT://kafka:9092
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=LISTENER_INT:PLAINTEXT,LISTENER_EXT:PLAINTEXT
      - KAFKA_LISTENERS=LISTENER_INT://0.0.0.0:9092,LISTENER_EXT://0.0.0.0:29092
      - KAFKA_INTER_BROKER_LISTENER_NAME=LISTENER_INT
      - KAFKA_JMX_PORT=49999
      - KAFKA_JMX_HOSTNAME=localhost
      - KAFKA_JMX_OPTS=-Djava.rmi.server.hostname=kafka -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false
      - KAFKA_OPTS=-javaagent:/usr/kafka/libs/jmx_prometheus_javaagent-0.18.0.jar=7071:/usr/kafka/libs/jmx-agent-config.yml
    volumes:
      - ./kafka-libs:/usr/kafka/libs

  exporter:
    image: danielqsj/kafka-exporter:latest
    ports: 
      - "9308:9308"
    command: "--kafka.server=kafka:9092 --no-sasl.handshake"
  
  prometheus:
    image: prom/prometheus:v2.36.2
    volumes:
      - ./prometheus/:/etc/prometheus/
      - ./prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    ports:
      - 9090:9090
  grafana:
    image: grafana/grafana
    user: "472"
    depends_on:
      - prometheus
    ports:
      - 3000:3000
    volumes:
      - ./grafana_data:/var/lib/grafana
      - ./grafana/provisioning/:/etc/grafana/provisioning/
    env_file:
      - ./grafana/config.monitoring
    restart: always
    
```
We will also create a default prometheus.yml file along with the docker-compose.yml. This configuration file contains all the configuration related to Prometheus. The config below is the default configuration which comes with Prometheus.

```
# my global config
global:
  scrape_interval:     15s # By default, scrape targets every 15 seconds.
  evaluation_interval: 15s # By default, scrape targets every 15 seconds.
  # scrape_timeout is set to the global default (10s).

  # Attach these labels to any time series or alerts when communicating with
  # external systems (federation, remote storage, Alertmanager).
  external_labels:
      monitor: 'my-project'

# Load and evaluate rules in this file every 'evaluation_interval' seconds.
rule_files:
  - 'alert.rules'
  # - "first.rules"
  # - "second.rules"

# alert
alerting:
  alertmanagers:
  - scheme: http
    static_configs:
    - targets:
      - "alertmanager:9093"

# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.

  - job_name: 'prometheus'

    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 15s

    static_configs:
         - targets: ['localhost:9090']
  - job_name: 'kafka-exporter'

    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 15s
  
    static_configs:
      - targets: ['broker:9308']

  - job_name: 'kafka'

    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 15s
  
    static_configs:
      - targets: ['kafka:7071']


```

Finally, we can run the “docker-compose up -d” to run our Prometheus, Grafana, Zookeeper and Kafka instances.


Plotting the monitoring visualization on Grafana
Now that we have configured Kafka JMX metrics to pipe into Prometheus, it's time to visualize it in Grafana. Browse to http://localhost:3000, log in using admin/admin and add the data source for Prometheus as shown below. Make sure you use the data source name as “Prometheus” since we will be referring to this data source name when we query in our Grafana dashboards.

One way to create a dashboard in Grafana is to manually configure the panels one by one or to kickstart our process, we can download the pre-configured dashboard from the Grafana dashboard site and import it into your Grafana.

Click on the Download JSON link and download the json file and import it into our Grafana as shown below:

Make sure to choose the correct data source, which is “Prometheus” in our case, and click on the Import button.

You should immediately see the dashboard reporting the following metrics from the Kafka instance:

CPU Usage
JVM Memory Used
Time spent in GC
Message in Per Topic
Bytes In Per Topic
Bytes Out Per Topic