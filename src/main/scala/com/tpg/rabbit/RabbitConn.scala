package com.tpg.rabbit

import akka.actor.{ActorRef, ActorSystem}
import com.github.sstone.amqp.Amqp.{AddQueue, QueueBind, QueueParameters}
import com.github.sstone.amqp.{Amqp, ConnectionOwner, Consumer}
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/** functionality to connect to RabbitMQ */
object RabbitConn {

  val config = ConfigFactory.load()
  val rabbitUri = config.getConfig("rabbit").getString("uri")
  val queueName = config.getConfig("rabbit").getString("queue")

  /**
   * sets up an actor to handle a connection to a rabbit queue
   * @param system the system the actor will live in
   * @param handler a messages handler
   */
  def setUpRabbit(system: ActorSystem, handler: ActorRef) = {

    val connFactory = new ConnectionFactory()
    connFactory.setUri(rabbitUri)
    val conn = system.actorOf(ConnectionOwner.props(connFactory, 1 second))

    val consumer = ConnectionOwner.createChildActor(conn, Consumer.props(handler, channelParams = None, autoack = false))
    Amqp.waitForConnection(system, consumer).await()

    consumer ! QueueBind(queue = queueName, exchange = "amq.direct", routing_key = "mkey")
    consumer ! AddQueue(QueueParameters(name = queueName, passive = false, durable = true, autodelete = false))
  }

}
