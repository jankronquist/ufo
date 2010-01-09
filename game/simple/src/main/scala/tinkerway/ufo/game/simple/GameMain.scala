package tinkerway.ufo.game.simple


import api.Size
import client.TheClientApp
import server.SimpleServer
import ufo.client.common.SimpleClient
import ufo.server.{Server, World}

import org.newdawn.slick.AppGameContainer

object Main {
  def main(args: Array[String]) {
    println("Main 1")
    val server = new SimpleServer()
    println("Main 2")
    new TheClientApp(server)
    println("Main 3")
    server.init()
    println("Main 4")
  }
}
