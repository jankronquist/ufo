package tinkerway.ufo.domain


import api._
import entity.{PropertyContainer, AbstractEntity, Property}

trait HasPosition extends PropertyContainer with MightHavePosition {
  object position extends Property[Position](null)
  def getPosition() : Option[Position] = {
    Some(position())
  }
}

trait HasHitPoints extends PropertyContainer {
  object hitPoints extends Property[Int](0)
}

trait Being extends HasPosition with HasHitPoints {
  
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