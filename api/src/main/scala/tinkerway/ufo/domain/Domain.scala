package tinkerway.ufo.domain


import tinkerway.ufo.api._
import tinkerway.ufo.entity.{PropertyContainer, AbstractEntity, Property}

trait HasPosition extends MightHavePosition {
  this : PropertyContainer =>
  object position extends Property[Position](null)
  def getPosition() : Option[Position] = {
    Some(position())
  }
}

trait HasHitPoints {
  this : PropertyContainer =>
  object hitPoints extends Property[Int](0)
}

trait HasBeingState {
  this : PropertyContainer =>
  object state extends Property[BeingState](Alive())
}


class BeingState

case class Alive() extends BeingState

case class Dead() extends BeingState 

trait Being extends PropertyContainer with HasPosition with HasHitPoints with HasBeingState {
  object controlledBy extends Property[ClientId](null)
}

trait Human extends Being with AbstractEntity  {
  val entityTypeId = EntityTypeId(1)
  val entityType = classOf[Human]
}

///////////////////////////

trait HasLocation extends PropertyContainer with MightHavePosition {
  object location extends Property[Location](null)
  def getPosition() : Option[Position] = {
    if (location().isInstanceOf[PositionLocation]) {
      Some(location().asInstanceOf[PositionLocation].position)
    } else {
      None
    }
  }
}

trait Item extends AbstractEntity with HasLocation {
  val entityType = classOf[Item]
}