package tinkerway.ufo.game.simple.server



import tinkerway.ufo.api._
import tinkerway.ufo.server._
import tinkerway.ufo.game.simple.domain.Domain.{Gun, HealingPotion, HumanBeing}
import tinkerway.ufo.domain.{HasLocation, HasPosition}
import tinkerway.ufo.io.XStreamServerConnector
import tinkerway.ufo.client.common.{EntityTypeContainer, SimpleClient, ClientEntity, FunctionEntityTypeContainer}

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

trait MyScenarioHandler extends ScenarioHandler {
  this : Server =>

  val entityTypes = new FunctionEntityTypeContainer
  entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with HumanBeing)
  entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with HealingPotion)
  entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with Gun)
  val npc : Npc = new Npc("npc", new XStreamServerConnector(this), entityTypes)

  def beforeClientConnected(client : Client) : Unit = {
    println("clientConnected : " + client.name)
  }

  def afterClientConnected(client : Client) : Unit = {
    if (client.name != "npc") {
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(1, 1), client) with HumanBeing)
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(3, 2), client) with HumanBeing)
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(6, 8), client) with HumanBeing)
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(2, 12), client) with HumanBeing)
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(12, 3), client) with HumanBeing)
      addEntity(new ServerItem(IdFactory.makeEntityId(), PositionLocation(Position(5, 5))) with ServerHealingPotion)
      addEntity(new ServerItem(IdFactory.makeEntityId(), PositionLocation(Position(7, 7))) with ServerGun)
      beginGame()
    } else if (client.name == "npc") {
      addEntity(new ServerBeing(IdFactory.makeEntityId(), Position(9, 1), client) with HumanBeing)
    }
  }
}

class Npc(val name : String, serverConnector : ServerConnector, entityTypeContainer : EntityTypeContainer) extends SimpleClient(entityTypeContainer) {
  val actionHandler = serverConnector.connect(name, this)
  
  override def receive(event : Event) = {
    super.receive(event)
    event match {
      case BeginTurn(clientId) => {
        if (clientId == this.clientId) {
          println("NPC turn!")
          actionHandler.perform(EndTurn())
        }
      }
      case _ =>
    }
  }
}

class SimpleServer extends Server(new World(new Size(25, 25))) with MyScenarioHandler {
}