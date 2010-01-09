package tinkerway.ufo.client.common


import api._
import entity._
import java.io.Serializable
import java.lang.reflect.Method
import java.util.Arrays
import scala.collection.mutable.HashMap

trait ClientEntity extends PropertyContainer with Serializable {
  def getProperty(name : String) : Property[Object] = {
    getAllProperties().filter(_.name.equals(name)).first
  }

}

class SimpleEntity(val entity : ClientEntity, val entityId : EntityId, val controlledBy : ClientId, initialProperties : List[PropertyValue], propertyChangeListener : PropertyChangeListener) {
  private val listener = new CompositePropertyChangeListener()
  listener.addPropertyChangeListener(propertyChangeListener)
  initialProperties.foreach(changeProperty(_))

  def changeProperty(property : PropertyValue) = {
    val prop = entity.getProperty(property.name)
    val old = prop()
    listener.propertyChange(new PropertyChange[Object](entity, property.name, old, property.value))

    prop := property.value
  }

  def getPropertyValue(name : String) : Object = {
    entity.getProperty(name).apply()
//    properties.get(name) match {
//      case Some(value) => value
//      case None => throw new IllegalArgumentException("no property named: " + name)
//    }
  }
}

trait EntityTypeContainer {
  def createEntity(entityTypeId : EntityTypeId) : ClientEntity
}

class FunctionEntityTypeContainer extends EntityTypeContainer {
  val entityTypes = new HashMap[EntityTypeId, () => ClientEntity]
  def createEntity(entityTypeId : EntityTypeId) : ClientEntity = {
    entityTypes.get(entityTypeId).get.apply()
  }
  def registerEntityType[T <: ClientEntity](function : () => T) = {
    entityTypes.put(function.apply().entityTypeId, function)
  }
}

class ClassEntityTypeContainer extends EntityTypeContainer {
  def createEntity(entityTypeId : EntityTypeId) : ClientEntity = {
    val entityType : Class[ClientEntity] = entityTypes.get(entityTypeId).get
    entityType.getConstructor().newInstance().asInstanceOf[ClientEntity]
  }

  val entityTypes = new HashMap[EntityTypeId, Class[ClientEntity]]

  def registerEntityType[T <: ClientEntity](entityType : Class[T]) = {
    val entity = entityType.getConstructor().newInstance(null).asInstanceOf[ClientEntity]
    entityTypes.put(entity.entityTypeId, entityType.asInstanceOf[Class[ClientEntity]])
  }
}

class SimpleClient(entityTypeContainer : EntityTypeContainer) extends EventListener  {
//  type EntityBase = ClientEntity
//  with EntityContainer
  var currentClient : ClientId = null
  var clientId : ClientId = null
  var world : WorldDescription = null
  val propertyChangeListener = new CompositePropertyChangeListener()
  val entities = new HashMap[EntityId, SimpleEntity]

  def getEntity(entityId : EntityId) : ClientEntity = {
    entities.get(entityId).get.entity
  }

  def receive(event : Event) = event match {

    case ConnectEvent(clientId, world) => {
      this.clientId = clientId
      this.world = world
    }

    case NewEntityEvent(entityId, entityTypeId, controlledBy, properties) => {
      entities.put(entityId, new SimpleEntity(entityTypeContainer.createEntity(entityTypeId), entityId, controlledBy, properties, propertyChangeListener))
    }

    case RemoveEntity(entityId)  => {
      entities.removeKey(entityId)
    }

    case PropertyChangeEvent(entityId, property)  => {
      entities.get(entityId).foreach(_.changeProperty(property))
    }

    case BeginTurn(clientId)  => {
      currentClient  = clientId
    }
  }


  def onPropertyChange[T](property : Property[T], function : Function3[ClientEntity, T, T, Unit]) = {
    propertyChangeListener.addPropertyChangeListener(new PropertyChangeListener() {
      def propertyChange[Y](event : PropertyChange[Y]) = {
        if (event.name.equals(property.name)) {
          function.apply(event.entity.asInstanceOf[ClientEntity], event.oldValue.asInstanceOf[T], event.newValue.asInstanceOf[T])
        }
      }
    })
  }
}
