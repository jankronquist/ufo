package tinkerway.ufo.api

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
  def connect(eventListener : EventListener) : ActionHandler
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

case class ClientId(id:Long)

case class EntityId(id:Long)

case class WorldDescription(size : Size, tiles : Array[TileTypeId])

case class PropertyValue(name : String, value : Object)

sealed abstract class Location

case class EntityLocation(entityId : EntityId) extends Location

case class PositionLocation(position : Position) extends Location

// EVENTS

case class ConnectEvent(clientId : ClientId, world : WorldDescription) extends Event

case class NewEntityEvent(entityId : EntityId, entityType : EntityTypeId, controlledBy : ClientId, properties : List[PropertyValue]) extends Event

case class RemoveEntity(entityId : EntityId) extends Event

case class PropertyChangeEvent(entityId : EntityId, property : PropertyValue) extends Event

case class BeginTurn(clientId : ClientId) extends Event

// TODO: client names, teams

// ACTIONS

sealed abstract class ItemActionType

case class Take() extends ItemActionType
case class Place() extends ItemActionType
case class Use() extends ItemActionType

case class EndTurn() extends Action

case class Move(beingId : EntityId, position : Position) extends Action

case class ItemAction(beingId : EntityId, action : ItemActionType, itemId : EntityId, location : Location) extends Action

case class BeginGame() extends Action

// action results

case class Successful() extends ActionResult

case class NotYourTurn() extends ActionResult

case class NotInYourControl() extends ActionResult

case class IllegalAction() extends ActionResult

case class EntityDoesNotExist() extends ActionResult