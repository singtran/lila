package lila.api

import akka.actor._
import java.lang.management.ManagementFactory
import kamon.Kamon.metrics
import scala.concurrent.duration._

import lila.socket.actorApi.NbMembers
import lila.hub.actorApi.round.NbRounds

private final class KamonPusher(
  countUsers: () => Int) extends Actor {

  import KamonPusher._

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'nbMembers, 'nbRounds)
    context.system.scheduler.schedule(1 second, 1 second, self, Tick)
  }

  private val threadStats = ManagementFactory.getThreadMXBean
  private val app = lila.common.PlayApp

  def receive = {

    case NbMembers(nb) =>
      metrics.histogram("socket.member") record nb

    case NbRounds(nb) =>
      metrics.histogram("round.member") record nb

    case Tick =>
      metrics.histogram("jvm.thread") record threadStats.getThreadCount
      metrics.histogram("jvm.daemon") record threadStats.getDaemonThreadCount
      metrics.histogram("jvm.uptime") record app.uptime.toStandardSeconds.getSeconds
      metrics.histogram("user.online") record countUsers()
  }
}

object KamonPusher {

  private case object Tick
}