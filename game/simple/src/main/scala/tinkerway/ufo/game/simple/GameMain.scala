package tinkerway.ufo.game.simple


import client.TheClientApp
import server.SimpleServer

import org.newdawn.slick.AppGameContainer

// -Djava.library.path=/Users/jan/Sdk/gfx/lwjgl-2.1.0/native/macosx/

object Main {
  def main(args: Array[String]) {
    val server = new SimpleServer()
    new TheClientApp(server)
    server.init()
  }
}
