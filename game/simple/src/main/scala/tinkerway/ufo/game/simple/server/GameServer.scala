package tinkerway.ufo.game.simple.server



import api._
import domain.Domain.{HealingPotion, HumanBeing}
import ufo.server._

trait ServerHealingPotion extends HealingPotion with Usable {
  def use(user : ServerBeing, location : Location) = {
    println("HEALING POSITION WAS USED!")
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
    Thread.sleep(5000)
//    addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(2, 3), clients(0)) with HumanBeing)
  }

}