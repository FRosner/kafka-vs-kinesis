package de.frosner.kvk.kinesis

import java.nio.ByteBuffer

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.kinesis.{KinesisFlowSettings, ShardSettings}
import akka.stream.alpakka.kinesis.scaladsl.{
  KinesisFlow,
  KinesisSink,
  KinesisSource
}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder
import com.amazonaws.services.kinesis.model.{
  PutRecordsRequestEntry,
  PutRecordsResultEntry,
  ShardIteratorType
}

import scala.concurrent.Await
import scala.concurrent.duration._

// https://developer.lightbend.com/docs/alpakka/current/kinesis.html
object SteamingApp extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val amazonKinesisAsync
    : com.amazonaws.services.kinesis.AmazonKinesisAsync =
    AmazonKinesisAsyncClientBuilder.defaultClient()

  system.registerOnTermination(amazonKinesisAsync.shutdown())

  val streamName = "terraform-kinesis-test"

  val settings = ShardSettings(streamName = streamName,
                               shardId = "shardId-000000000000",
                               shardIteratorType =
                                 ShardIteratorType.TRIM_HORIZON,
                               refreshInterval = 1.second,
                               limit = 500)

  val source: Source[com.amazonaws.services.kinesis.model.Record, NotUsed] =
    KinesisSource.basic(settings, amazonKinesisAsync)

  val flowSettings = KinesisFlowSettings(
    parallelism = 1,
    maxBatchSize = 500,
    maxRecordsPerSecond = 1000,
    maxBytesPerSecond = 1000000,
    maxRetries = 5,
    backoffStrategy = KinesisFlowSettings.Exponential,
    retryInitialTimeout = 100.millis
  )

  val flow: Flow[PutRecordsRequestEntry, PutRecordsResultEntry, NotUsed] =
    KinesisFlow(streamName, flowSettings)

  val sink: Sink[PutRecordsRequestEntry, NotUsed] =
    KinesisSink(streamName, flowSettings)

  val now = System.currentTimeMillis()
  val doneProducing = Source(1 to 100)
    .map(_.toString)
    .map { elem =>
      val entry = new PutRecordsRequestEntry()
      entry.setData(ByteBuffer.wrap(elem.getBytes()))
      entry.setPartitionKey(elem)
      entry
    }
    .runWith(sink)
  println(s"Produced in ${System.currentTimeMillis() - now} ms")

  val doneConsuming =
    source
      .runWith(Sink.foreach(x => println(new String(x.getData.array()))))

}
