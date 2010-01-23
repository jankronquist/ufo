package tinkerway.ufo.game.simple


import client.TheClientApp
import server.SimpleServer

import org.newdawn.slick.AppGameContainer
import tinkerway.ufo.api._
import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}
// -Djava.library.path=/Users/jan/Sdk/gfx/lwjgl-2.1.0/native/macosx/

class SerializingServerConnector(connector : ServerConnector) extends ServerConnector {
  private def makeSerialized[T](obj : T) = {
    val byteOut = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(byteOut)
    out.writeObject(obj)
    val byteIn = new ByteArrayInputStream(byteOut.toByteArray)
    val in = new ObjectInputStream(byteIn)
    in.readObject().asInstanceOf[T]
    
  }
  class SerializingEventListener(eventListener : EventListener) extends EventListener {
    def receive(event : Event) = {
      eventListener.receive(makeSerialized(event))
    }
  }
  class SerializingActionHandler(actionHandler : ActionHandler) extends ActionHandler {
    def perform(action : Action) : ActionResult = {
      makeSerialized(actionHandler.perform(makeSerialized(action)))
    }

  }
  def connect(eventListener : EventListener) : ActionHandler = {
    new SerializingActionHandler(connector.connect(new SerializingEventListener(eventListener)))
  }
}

object Main {
  def main(args: Array[String]) {
    val server = new SimpleServer()
    new TheClientApp(new SerializingServerConnector(server))
    server.init()
  }
}
