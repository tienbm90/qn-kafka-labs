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

