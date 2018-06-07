package de.frosner.kvk.kafka

import java.util.concurrent.atomic.AtomicLong

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object StreamingApp extends App {

  class DB {

    private val offset = new AtomicLong

    def save(record: ConsumerRecord[Array[Byte], String]): Future[Done] = {
      println(s"DB.save: ${record.value}")
      offset.set(record.offset)
      Future.successful(Done)
    }

    def loadOffset(): Future[Long] =
      Future.successful(offset.get)

    def update(data: String): Future[Done] = {
      println(s"DB.update: $data")
      Future.successful(Done)
    }
  }

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val topic = "logs"

  val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  val doneProducing = Source(1 to 100)
    .map(_.toString)
    .map { elem =>
      new ProducerRecord[String, String](topic, elem, elem)
    }
    .runWith(Producer.plainSink(producerSettings))

  val now = System.currentTimeMillis()
  Await.ready(doneProducing, 500.seconds)
  println(s"Produced in ${System.currentTimeMillis() - now} ms")

  val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val db = new DB

  val doneConsuming =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic))
      .mapAsync(1) { msg =>
        db.update(msg.record.value).map(_ => msg)
      }
      .mapAsync(1) { msg =>
        msg.committableOffset.commitScaladsl()
      }
      .runWith(Sink.ignore)

}
