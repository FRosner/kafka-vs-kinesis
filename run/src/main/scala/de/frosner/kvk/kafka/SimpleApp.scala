package de.frosner.kvk.kafka

import cakesolutions.kafka.{KafkaConsumer, KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.{Conf => ProducerConf}
import cakesolutions.kafka.KafkaConsumer.{Conf => ConsumerConf}
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This app writes a single message to the `log` topic and consumes it again.
  */
object SimpleApp extends App {

  val topic = "logs"

  val producer = KafkaProducer(
    ProducerConf(keySerializer = new StringSerializer(),
                 valueSerializer = new StringSerializer(),
                 bootstrapServers = "localhost:9092")
  )

  val consumer = KafkaConsumer(
    ConsumerConf(keyDeserializer = new StringDeserializer(),
                 valueDeserializer = new StringDeserializer(),
                 bootstrapServers = "localhost:9092",
                 groupId = "1")
  )
  println(s"Subscribing to $topic")
  consumer.subscribe(List(topic).asJava)

  val record = KafkaProducerRecord(topic, Some("key"), "value")

  println(s"Sending $record")
  val produced = producer.send(record)
  Await.ready(produced, 5.seconds)
  println(s"Finished sending $record")

  Stream
    .continually(consumer.poll(1000))
    .take(100)
    .filter(!_.isEmpty)
    .foreach(_.iterator().asScala.foreach(println))

  consumer.unsubscribe()
  producer.close()
}
