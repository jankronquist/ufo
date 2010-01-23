package tinkerway.ufo.game.simple


import client.TheClientApp
import domain.Domain.HumanBeing
import server.SimpleServer

import org.newdawn.slick.AppGameContainer
import tinkerway.ufo.api._
import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}
import com.thoughtworks.xstream.XStream
import tinkerway.ufo.entity.{EntityContainer, Entity}
import tinkerway.ufo.server.{ServerEntityContainer, ServerBeing}
import com.thoughtworks.xstream.io.json.{JettisonMappedXmlDriver, JsonHierarchicalStreamDriver}
import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, SingleValueConverter, Converter}
import com.thoughtworks.xstream.io.{HierarchicalStreamWriter, HierarchicalStreamReader}
import com.thoughtworks.xstream.mapper.{Mapper, MapperWrapper}
import tinkerway.ufo.io.SerializingServerConnector
// -Djava.library.path=/Users/jan/Sdk/gfx/lwjgl-2.1.0/native/macosx/


object Main {
  def main(args: Array[String]) {
    val server = new SimpleServer()
    new TheClientApp(new SerializingServerConnector(server))
    server.init()
  }
}
