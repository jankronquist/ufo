package tinkerway.ufo.client.common


import tinkerway.ufo.api._
import tinkerway.ufo.entity._
import java.io.Serializable
import java.lang.reflect.Method
import java.util.Arrays
import scala.collection.mutable.HashMap

abstract class ClientEntity(val entityId : EntityId) extends AbstractEntity with Serializable {
  def getProperty(name : String) : Property[Object] = {
    getAllProperties().filter(_.name.equals(name)).first
  }

  def changeProperty(property : PropertyValue) = {
    val prop = getProperty(property.name)
    val old = prop()
//    propertyChange(new PropertyChange[Object](this, property.name, old, property.value))

    prop := property.value
  }

}

trait EntityTypeContainer {
  def createEntity(entityTypeId : EntityTypeId, entityId : EntityId) : ClientEntity
}

class FunctionEntityTypeContainer extends EntityTypeContainer {
  val entityTypes = new HashMap[EntityTypeId, (EntityId) => ClientEntity]
  def createEntity(entityTypeId : EntityTypeId, entityId : EntityId) : ClientEntity = {
    entityTypes.get(entityTypeId).get.apply(entityId)
  }
  def registerEntityType[T <: ClientEntity](function : (EntityId) => T) = {
    entityTypes.put(function.apply(EntityId(-1)).entityTypeId, function)
  }
}

class SimpleClient(entityTypeContainer : EntityTypeContainer) extends EventListener with ClientEntityContainer {
//  type EntityBase = ClientEntity
//  with EntityContainer
  var currentClient : ClientId = null
  var clientId : ClientId = null
  var world : WorldDescription = null
  val propertyChangeListener = new CompositePropertyChangeListener()

  def receive(event : Event) = event match {

    case ConnectEvent(clientId, world) => {
      this.clientId = clientId
      this.world = world
    }

    case NewEntityEvent(entityId, entityTypeId, properties) => {
      val entity = entityTypeContainer.createEntity(entityTypeId, entityId)
      entity.addPropertyChangeListener(propertyChangeListener)
      properties.foreach(entity.changeProperty(_))
      internalAddEntity(entityId, entity)
    }

    case RemoveEntity(entityId)  => {
      removeEntity(entityId)
    }

    case PropertyChangeEvent(entityId, property)  => {
      findEntity(entityId).changeProperty(property)
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

trait ClientEntityContainer extends EntityContainer {
  type EntityBase = ClientEntity

}
