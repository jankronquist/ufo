package tinkerway.ufo.game.simple.server



import tinkerway.ufo.api._
import tinkerway.ufo.server._
import tinkerway.ufo.game.simple.domain.Domain.{Gun, HealingPotion, HumanBeing}
import tinkerway.ufo.domain.{HasLocation, HasPosition}

trait ServerHealingPotion extends HealingPotion with Usable {
  def use(user : ServerBeing, location : Location) = {
    println("HEALING POSITION WAS USED!")
  }
}

trait ServerGun extends Gun with Usable {
  def findPosition(location : Location) : Position = {
    location match {
      case PositionLocation(pos) => pos
      case EntityLocation(entity) => entity match {
        case e : HasPosition =>  e.position()
        case e : HasLocation => findPosition(e.location())
        case _ => throw new IllegalStateException("failed to find position!")
      }
    }
  }
  def use(user : ServerBeing, location : Location) = {
    println("Fire weapon, target=" + findPosition(location))
  }
}


class SimpleServer extends Server(new World(new Size(25, 25))) {

  def init() = {
    val being1 = IdFactory.makeEntityId()
    addEntity(new ServerBeing(being1, Position(1, 1), clients(0)) with HumanBeing)
    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(3, 2), clients(0)) with HumanBeing)
    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(6, 8), clients(0)) with HumanBeing)
    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(2, 12), clients(0)) with HumanBeing)
    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(12, 3), clients(0)) with HumanBeing)
    addEntity(new ServerItem(IdFactory.makeEntityId(), PositionLocation(Position(5, 5))) with ServerHealingPotion)
    addEntity(new ServerItem(IdFactory.makeEntityId(), PositionLocation(Position(7, 7))) with ServerGun)
    Thread.sleep(5000)
//    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(2, 3), clients(0)) with HumanBeing)
  }

}