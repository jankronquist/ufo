package tinkerway.ufo.game.simple


import client.TheClientApp
import server.SimpleServer
import tinkerway.ufo.io.{XStreamServerConnector}

// -Djava.library.path=/Users/jan/Sdk/gfx/lwjgl-2.1.0/native/macosx/


object Main {
  def main(args: Array[String]) {
    val server = new SimpleServer()
//    new TheClientApp(new SerializingServerConnector(server))
    new TheClientApp(new XStreamServerConnector(server))
  }
}
