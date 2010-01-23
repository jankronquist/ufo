package tinkerway.ufo.io

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import tinkerway.ufo.api._
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.mapper.{MapperWrapper, Mapper}
import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, Converter, SingleValueConverter}
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import tinkerway.ufo.entity.{Entity, EntityContainer}

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

class EntityConverter(entityContainer : EntityContainer) extends SingleValueConverter {
  def canConvert(clazz : java.lang.Class[_]) = {
    classOf[Entity].isAssignableFrom(clazz)
  }
  def fromString(str : String) : Object = {
    val id = EntityId(java.lang.Long.parseLong(str))
    entityContainer.findEntity(id)
  }

  def toString(obj : Object) : String = {
    obj.asInstanceOf[Entity].entityId.id.toString()
  }
}
/*
class EntityConverter(entityContainer : EntityContainer) extends Converter {
  def canConvert(clazz : java.lang.Class[_]) = {
    classOf[Entity].isAssignableFrom(clazz)
  }
  def marshal(source : Object, writer : HierarchicalStreamWriter, context : MarshallingContext) = {
    writer.setValue(source.asInstanceOf[Entity].entityId.id.toString())
  }
  def unmarshal(reader : HierarchicalStreamReader, context : UnmarshallingContext) : Object = {
    val id = EntityId(java.lang.Long.parseLong(reader.getValue()))
    entityContainer.findEntity(id)
  }
}
*/

class EntityMapperWrapper(mapper:Mapper) extends MapperWrapper(mapper) {
  override def serializedClass(clazz : Class[_]) : String = {
    if (classOf[Entity].isAssignableFrom(clazz)) {
      "entity"
    } else {
      super.serializedClass(clazz)
    }
  }
  override def realClass(elementName : String) : Class[_] = {
    if (elementName.equals("entity")) {
      classOf[Entity]
    } else {
      super.realClass(elementName)
    }
  }
}

class EntityXStream(entityContainer : EntityContainer) extends XStream(new JettisonMappedXmlDriver()) {
  registerConverter(new EntityConverter(entityContainer))
  override def wrapMapper(next :MapperWrapper ) : MapperWrapper = {
      new EntityMapperWrapper(next)
  }
}
