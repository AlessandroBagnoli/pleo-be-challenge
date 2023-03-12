package io.pleo.antaeus.core

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun buildPublisher(
  project: String,
  topic: String,
  host: String = System.getenv("PUBSUB_EMULATOR_HOST")
): Publisher {
  val topicName = TopicName.of(project, topic)
  log.info { "Using pubsub located at $host" }

  val channel = ManagedChannelBuilder.forTarget(host).usePlaintext().build()
  val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
  return Publisher.newBuilder(topicName)
    .setEnableMessageOrdering(true)
    .setChannelProvider(channelProvider)
    .setCredentialsProvider(NoCredentialsProvider.create())
    .build()
}

fun buildSubscriber(
  project: String,
  subscription: String,
  host: String = System.getenv("PUBSUB_EMULATOR_HOST"),
  handler: MessageReceiver
): Subscriber {
  val subscriptionName = ProjectSubscriptionName.of(project, subscription)
  log.info { "Using pubsub located at $host" }

  val channel = ManagedChannelBuilder.forTarget(host).usePlaintext().build()
  val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
  return Subscriber.newBuilder(subscriptionName, handler)
    .setChannelProvider(channelProvider)
    .setCredentialsProvider(NoCredentialsProvider.create())
    .build()
}