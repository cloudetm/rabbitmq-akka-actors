package com.tpg.scala

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit._
import com.tpg.scala.rabbithandler.RabbitQueueSource.{GetQueueSize, RabbitMsg, RabbitQueueSourceActor}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class RabbitQueueSourceActorTest extends TestKit(ActorSystem("RabbitQueueSourceSpec"))
                            with DefaultTimeout with ImplicitSender
                            with WordSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll() {
    shutdown()
  }


  "A RabbitQueueSourceActor" must {

    "start with an empty queue" in {
      val ar = TestActorRef(new RabbitQueueSourceActor)
      val a = ar.underlyingActor

      assert(a.rabbitMsgQueue.size == 0)
    }


    "have one message queued after receiving a message" in {
      val ar = TestActorRef(new RabbitQueueSourceActor)
      val a = ar.underlyingActor

      ar ! new RabbitMsg("test")

      assert(a.rabbitMsgQueue.size == 1)
    }


    "empty its queue if requested by stream" in {
      implicit val materializer = ActorMaterializer()

      val ra = system.actorOf(Props(classOf[RabbitQueueSourceActor]))
      val pub = ActorPublisher[RabbitMsg](ra)
      Source(pub).runWith(Sink.ignore)

      ra ! new RabbitMsg("test")
      ra ! new Request(1L)

      ra ! GetQueueSize
      expectMsg(0)
    }

  }


}
