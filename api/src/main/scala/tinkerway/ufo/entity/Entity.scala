package tinkerway.ufo.entity

import collection.mutable.HashMap
import java.io.Serializable
import java.lang.reflect.Method
import tinkerway.ufo.api._

case class PropertyChange[T](entity : Entity, name : String, oldValue : T, newValue : T)

trait PropertyChangeListener extends Serializable {
  def propertyChange[T](event : PropertyChange[T])
}

class CompositePropertyChangeListener extends PropertyChangeListener {
  private var propertyChangeListeners : List[PropertyChangeListener] = Nil
  def addPropertyChangeListener(pcl : PropertyChangeListener) = {
    propertyChangeListeners = pcl :: propertyChangeListeners
  }

  def propertyChange[T](event : PropertyChange[T]) = {
    propertyChangeListeners.foreach(_.propertyChange(event))
  }
}

trait Entity extends CompositePropertyChangeListener {
  val entityTypeId : EntityTypeId
  val entityId : EntityId
}


class Property[T](private var currentValue : T)(implicit entity : Entity) extends Serializable {
  private val listener = new CompositePropertyChangeListener()

  val name = getName()
  def apply() = currentValue
  def :=(newValue:T) : Unit = {
    listener.propertyChange(new PropertyChange(entity, name, currentValue, newValue))
    currentValue = newValue
  }

  def addPropertyChangeListener(pcl : PropertyChangeListener) = {
    listener.addPropertyChangeListener(pcl)
  }

  private def getName() = {
    val simpleName = this.getClass().getSimpleName()
    simpleName.substring (simpleName.indexOf('$')+1, simpleName.length()-1)
  }

  listener.addPropertyChangeListener(entity)

  override def toString() = {
    name + "=" + currentValue
  }
}

trait PropertyContainer extends Entity {
  implicit val entity : Entity = this
  private var properties : List[Property[Object]] = Nil

  protected def getAllProperties() = properties

  // code taken from lift web MetaMapper initialization
  private def init() = {
    def addProperty[T](property : Property[T]) = {
      properties = property.asInstanceOf[Property[Object]] :: properties
    }

    def isMagicObject(m: Method) = m.getReturnType.getName.endsWith("$"+m.getName+"$") && m.getParameterTypes.length == 0
    def isMappedField(m: Method) = classOf[Property[Object]].isAssignableFrom(m.getReturnType)

    val propertyMethods = this.getClass.getMethods.toList.filter(m => isMagicObject(m) && isMappedField(m))

    propertyMethods.foreach(m => addProperty(m.invoke(this).asInstanceOf[Property[Object]]))
  }
  init()

  def onChange[T](property : Property[T], function : Function2[T, T, Unit]) : Unit = {
    addPropertyChangeListener(new PropertyChangeListener() {
      def propertyChange[Y](event : PropertyChange[Y]) = {
        if (event.name.equals(property.name)) {
          function.apply(event.oldValue.asInstanceOf[T], event.newValue.asInstanceOf[T])
        }
      }
    })
  }

  def onChange[T](property : Property[T], function : Function1[T, Unit]) : Unit = {
    onChange(property, (from:T, to:T) => function.apply(to))
  }


}

trait AbstractEntity extends Entity with PropertyContainer {
//  val entityType : Class[T]
}



class CapturePropertyChangeListener extends PropertyChangeListener {
  private var events : List[PropertyChange[Object]] = Nil
  def propertyChange[T](event : PropertyChange[T]) = {
    events = event.asInstanceOf[PropertyChange[Object]] :: events
  }
  def take() : PropertyChange[Object] = events match {
    case head :: tail => {
      events = tail
      head
    }
    case Nil => {
      throw new IllegalStateException("No more events")
    }
  }
}

trait EntityListener[EntityBase <: Entity] {
  def entityAdded(entity : EntityBase)
  def entityRemoved(entity : EntityBase)
}

class CompositeEntityListener[EntityBase <: Entity] extends EntityListener[EntityBase] {
  private var listeners : List[EntityListener[EntityBase]] = Nil
  
  def entityAdded(entity : EntityBase) = {
    listeners.foreach(_.entityAdded(entity))
  }
  def entityRemoved(entity : EntityBase) = {
    listeners.foreach(_.entityRemoved(entity))
  }

  def addEntityListener(el : EntityListener[EntityBase]) = {
    listeners = el :: listeners
  }
}

trait EntityContainer {
  type EntityBase <: Entity
  private val entities = new HashMap[EntityId, EntityBase]()
  private val entityListener = new CompositeEntityListener[EntityBase]

  def addEntityListener(el : EntityListener[EntityBase]) = {
    entityListener.addEntityListener(el)
  }

  def removeEntity(entityId : EntityId) : Unit = {
    entities.removeKey(entityId) match {
      case Some(entity) => entityListener.entityRemoved(entity)
      case _ => 
    }
  }

  def internalAddEntity(entityId : EntityId, entity : EntityBase) : Unit = {
    if (entities.contains(entityId)) {
      throw new IllegalArgumentException("Duplicate id: " + entityId)
    }
    entityListener.entityAdded(entity)
    entities.put(entityId, entity)
  }

  def findEntity(position : Position) : Option[EntityBase] = {
    entities.values.find(e => {
      if (e.isInstanceOf[MightHavePosition]) {
        e.asInstanceOf[MightHavePosition].getPosition.equals(Some(position))
      } else {
        false
      }
    })
  }

  def findEntity(entityId : EntityId) : EntityBase = {
    entities.get(entityId) match {
      case Some(entity) => entity
      case None => throw new IllegalArgumentException("Entity does not exists")
    }
  }

  def getAllEntities() = entities.values
}
