package tinkerway.ufo.api

import java.io.Serializable

trait Event

trait Action

trait ActionResult

trait ActionHandler {
  def perform(action : Action) : ActionResult
}

trait EventListener {
  def receive(event : Event)
}

trait ServerConnector {
  def connect(name : String, eventListener : EventListener) : ActionHandler
}

case class Size(x:Int, y:Int)

case class Position(x:Int, y:Int) {
  def isNextTo(other : Position) : Boolean = {
    if (Math.abs(x-other.x) + Math.abs(y-other.y) == 1) {
      true
    } else {
      false
    }
  }
}

trait MightHavePosition {
  def getPosition() : Option[Position]
}

case class TileTypeId(id:Long)

case class EntityTypeId(id:Long)

case class EffectTypeId(id:Long)

case class ClientId(id:Long)

case class EntityId(id:Long)

case class WorldDescription(size : Size, tiles : Array[TileTypeId])

case class PropertyValue(name : String, value : Object)

sealed abstract class Location

case class EntityLocation(entity : Entity) extends Location

case class PositionLocation(position : Position) extends Location

abstract class Effect

case class LinearEffect(effectTypeId : EffectTypeId, from : Position, to : Position) extends Effect

// EVENTS

case class ConnectEvent(clientId : ClientId, world : WorldDescription) extends Event

case class NewEntityEvent(entityId : EntityId, entityType : EntityTypeId, properties : List[PropertyValue]) extends Event

case class RemoveEntity(entityId : EntityId) extends Event

case class PropertyChangeEvent(entityId : EntityId, property : PropertyValue) extends Event

case class BeginTurn(clientId : ClientId) extends Event

case class EffectEvent(effect : Effect) extends Event

// TODO: client names, teams

// ACTIONS

sealed abstract class ItemActionType

case class Place() extends ItemActionType
case class Use() extends ItemActionType

case class EndTurn() extends Action

case class Move(being : Entity, position : Position) extends Action

case class ItemAction(being : Entity, action : ItemActionType, itemI : Entity, location : Location) extends Action

case class BeginGame() extends Action

// action results

case class Successful() extends ActionResult

case class NotYourTurn() extends ActionResult

case class NotInYourControl() extends ActionResult

case class IllegalAction() extends ActionResult

case class NotEnoughActionPoints() extends ActionResult

case class EntityDoesNotExist() extends ActionResult

// entities

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
