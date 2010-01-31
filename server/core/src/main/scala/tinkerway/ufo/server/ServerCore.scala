package tinkerway.ufo.server

import tinkerway.ufo.entity._
import tinkerway.ufo.api._
import tinkerway.ufo.domain.{Alive, Item, Being, HasPosition}

class Client(val clientId : ClientId, val name : String, val eventListener : EventListener) {
  
}

abstract class ServerEntity(val entityId : EntityId) extends AbstractEntity {
  def getAllPropertyValues() : List[PropertyValue] = {
    getAllProperties().map(p => new PropertyValue(p.name, p.apply()))
  }
}

abstract class ServerBeing(entityId : EntityId, initialPosition : Position, initialControlledBy : Client) extends ServerEntity(entityId) with Being {
  position := initialPosition
  controlledBy := initialControlledBy.clientId

  def assertControlledBy(client : Client) = {
    if (client.clientId != controlledBy()) {
      throw ActionException(NotInYourControl())
    }
  }
  def moveTo(target : Position) = {
    if (!position().isNextTo(target)) {
      throw ActionException(IllegalAction())
    }
    position := target
  }

  private var inventory : List[ServerItem] = Nil

  def assertCanPerformAction() = {
    state() match {
      case Alive() => true
      case _ => throw new ActionException(IllegalAction())
    }
  }

  def performItemAction(action : ItemActionType, item : ServerItem, location : Location) = action match {
    case Place() => {
      if (!inventory.contains(item)) {
        item.location() match {
          case EntityLocation(entityId) => throw ActionException(IllegalAction())
          case PositionLocation(itemPosition) => {
            if (!(this.position().equals(itemPosition) || this.position().isNextTo(itemPosition))) {
              throw ActionException(IllegalAction())
            }
          }
        }
        inventory = item :: inventory
        item.location := new EntityLocation(this)
      } else {
        assertItemInInventory(item)
        inventory = inventory.remove(_ == item)
        item.location := new PositionLocation(position())
        // TODO: check location (ie where the item was dropped) allow drop on entity
      }
    }
    case Use() => {
      assertItemInInventory(item)
      item.use(this, location)
    }
  }

  private def assertItemInInventory(item : ServerItem) = {
    if (!inventory.contains(item)) {
      throw ActionException(IllegalAction())
    }
  }
}

trait Usable {
  def use(user : ServerBeing, location : Location)
}

abstract class ServerItem(entityId : EntityId, initialLocation : Location) extends ServerEntity(entityId) with Usable with Item {
  location := initialLocation
}

case class ActionException(failure : ActionResult) extends RuntimeException 

object IdFactory {
  private var nextId : Long = 0
  
  def makeClientId() = {
    nextId = nextId + 1
    ClientId(nextId)
  }

  def makeEntityId() = {
    nextId = nextId + 1
    EntityId(nextId)
  }

}

class Tile(val tileType : TileTypeId) {
  
}

class World(val size : Size) {
  val tiles = new Array[Tile](size.x * size.y).map(x => new Tile(TileTypeId(1)))

  def describe() : WorldDescription = {
    WorldDescription(size, tiles.map(_.tileType))
  }
}

class Turn(clients : List[Client]) {
  var remainingClients : List[Client] = clients
  var currentClient : Client = null

  def hasNext() : Boolean = {
    remainingClients.length > 0
  }

  def nextClient() : Client = remainingClients match {
    case head :: tail => {
      remainingClients = tail
      currentClient = head
      currentClient
    }
    case _ => {
      throw new IllegalStateException("no remaining clients")
    }
    
  }

  def isCurrentClient(client : Client) = {
    currentClient == client
  }
}

trait ScenarioHandler {
  def beforeClientConnected(client : Client) : Unit
  def afterClientConnected(client : Client) : Unit
}

trait ServerState {
  def perform(client : Client, action : Action) : ActionResult
}

trait ServerSPI {
  def beginGame() : Unit
  def addEntity(entity : ServerEntity) : Unit
  def sendToAll(event : Event) : Unit
}

class Server(val world : World) extends ServerEntityContainer with ServerConnector with ServerSPI {
  scenarioHandler : ScenarioHandler =>
  
  var clients : List[Client] = Nil
  private var turn : Turn = null
  var state : ServerState = new LobbyServerState()

  class ServerActionHandler(client : Client) extends ActionHandler {
    def perform(action : Action) : ActionResult =  {
      state.perform(client, action)
    }
  }

  class LobbyServerState extends ServerState {
    def perform(client : Client, action : Action) : ActionResult =  {
      if (action.equals(BeginGame())) {
        beginGame()
        Successful()
      } else {
        IllegalAction()
      }
    }
  }

  class TurnBasedServerState extends ServerState {
    def perform(client : Client, action : Action) : ActionResult =  {
      if (action.equals(BeginGame())) {
        IllegalAction()
      } else if (turn.isCurrentClient(client)) {
        try {
          doPerform(client, action)
          Successful()
        } catch  {
          case ActionException(failure) => failure
        }
      } else {
        NotYourTurn()
      }
    }

    def doPerform(client : Client, action : Action) = action match {
      case EndTurn() => {
        nextClient()
      }
      case Move(being : ServerBeing, position) => {
        being.assertControlledBy(client)
        if (being.actionPoints() < 1) {
          throw new ActionException(NotEnoughActionPoints())
        }
        if (position.x < 0 || position.y < 0 || position.x >= world.size.x || position.y >= world.size.y) {
          throw new ActionException(IllegalAction())
        }
        // TODO: what if the entity is passable? do we need support?
        if (findEntity(position) != None) {
          throw new ActionException(IllegalAction())
        }
        // TODO: check tiles
        being.moveTo(position)

        // TODO: better way to handle action points
        being.actionPoints := being.actionPoints() - 1
      }
      case ItemAction(being : ServerBeing, action, item : ServerItem, location) => {
        being.assertControlledBy(client)
        if (being.actionPoints() < 1) {
          throw new ActionException(NotEnoughActionPoints())
        }
        being.performItemAction(action, item, location)
        being.actionPoints := being.actionPoints() - 1
      }
    }
  }

  def connect(name : String, eventListener : EventListener) : ActionHandler = {
    val clientId = IdFactory.makeClientId()
    eventListener.receive(ConnectEvent(clientId, world.describe()))
    val client = new Client(clientId, name, eventListener)
    scenarioHandler.beforeClientConnected(client)
    clients = client :: clients
    getAllEntities().foreach(e => eventListener.receive(makeNewEntityEvent(e)))
    scenarioHandler.afterClientConnected(client)
    new ServerActionHandler(client)
  }

  def beginGame() = {
    newTurn()
    state = new TurnBasedServerState()
  }

  def newTurn() : Unit = {
    if (clients.length == 0) {
      throw new IllegalStateException("No clients connected")
    }
    turn = new Turn(clients)
    getAllEntities().filter(_.isInstanceOf[Being]).foreach(_.asInstanceOf[Being].resetActionPoints())
    nextClient()
  }

  def nextClient() = {
    if (turn.hasNext()) {
      val client = turn.nextClient()
      sendToAll(BeginTurn(client.clientId))
    } else {
      newTurn()
    }
  }

  // this method is only here for testing
  def getClient(clientId : ClientId) = {
    clients.find(_.clientId == clientId).get
  }

  val propertyChangeListener = new Object with PropertyChangeListener {
    def propertyChange[T](event : PropertyChange[T]) = {
      val msg = new PropertyChangeEvent(event.entity.asInstanceOf[ServerEntity].entityId, PropertyValue(event.name, event.newValue.asInstanceOf[Object]))
      sendToAll(msg)
    }
  }

  def addEntity(entity : ServerEntity) : Unit = {
    internalAddEntity(entity.entityId, entity)
    sendToAll(makeNewEntityEvent(entity))
    entity.addPropertyChangeListener(propertyChangeListener)
  }

  def makeNewEntityEvent(entity : ServerEntity) = NewEntityEvent(entity.entityId, entity.entityTypeId, entity.getAllPropertyValues())

  def sendToAll(event : Event) = {
    clients.foreach(_.eventListener.receive(event))
  }
}

trait ServerEntityContainer extends EntityContainer {
  type EntityBase = ServerEntity

  def findBeing(beingId : EntityId) : ServerBeing = {
    findEntity(beingId).asInstanceOf[ServerBeing]
  }

  def findItem(itemId : EntityId) : ServerItem = {
    findEntity(itemId).asInstanceOf[ServerItem]
  }

}
