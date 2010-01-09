package tinkerway.ufo.server

import collection.mutable.HashMap
import domain.{Item, Being, HasPosition}
import entity._
import tinkerway.ufo.api._

class Client(val clientId : ClientId, val eventListener : EventListener) {
  
}

abstract class ServerEntity(val entityId : EntityId) extends AbstractEntity {
  def getAllPropertyValues() : List[PropertyValue] = {
    getAllProperties().map(p => new PropertyValue(p.name, p.apply()))
  }
}

abstract class ServerBeing(entityId : EntityId, initialPosition : Position, var controlledBy : Client) extends ServerEntity(entityId) with Being {
  position := initialPosition

  def assertControlledBy(client : Client) = {
    if (client != controlledBy) {
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

  def performItemAction(action : ItemActionType, item : ServerItem, location : Location) = action match {
    case Take() => {
      item.location() match {
        case EntityLocation(entityId) => throw ActionException(IllegalAction())
        case PositionLocation(itemPosition) => {
          if (!this.position().equals(itemPosition)) {
            throw ActionException(IllegalAction())
          }
        }
      }
      inventory = item :: inventory
      item.location := new EntityLocation(entityId)
    }
    case Place() => {
      assertItemInInventory(item)
      inventory = inventory.remove(_ == item)
      item.location := new PositionLocation(position())
      // TODO: check location (ie where the item was dropped) allow drop on entity
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

class Server(world : World) extends ServerEntityContainer with ServerConnector {
  
  var clients : List[Client] = Nil
  private var turn : Turn = null

  class ServerActionHandler(client : Client) extends ActionHandler {
    def perform(action : Action) : ActionResult =  {
      if (action.equals(BeginGame())) {
        if (turn != null) {
          IllegalAction()
        } else {
          newTurn()
          Successful()
        }
      } else if (turn.isCurrentClient(client)) {
        try {
          doPerform(action)
          Successful()
        } catch  {
          case ActionException(failure) => failure
        }
      } else {
        NotYourTurn()
      }
    }
    
    def doPerform(action : Action) = action match {
      case EndTurn() => {
        nextClient()
      }
      case Move(beingId, position) => {
        val being = findBeingControlledByClient(beingId)
        if (position.x < 0 || position.y < 0 || position.x >= world.size.x || position.y >= world.size.y) {
          throw new ActionException(IllegalAction())
        }
        // TODO: what if the entity is passable? do we need support?
        if (findEntity(position) != None) {
          throw new ActionException(IllegalAction())
        }
        // TODO: check tiles
        being.moveTo(position)
      }
      case ItemAction(beingId, action, itemId, location) => {
        val being = findBeingControlledByClient(beingId)
        being.performItemAction(action, findItem(itemId), location)
      }
    }
    
    def findBeingControlledByClient(beingId : EntityId) : ServerBeing = {
      val being = findBeing(beingId)
      being.assertControlledBy(client)
      being
    }
  }

  def connect(eventListener : EventListener) : ActionHandler = {
    val clientId = IdFactory.makeClientId()
    eventListener.receive(ConnectEvent(clientId, world.describe()))
    val client = new Client(clientId, eventListener)
    clients = client :: clients 
    new ServerActionHandler(client)
  }

  def newTurn() : Unit = {
    if (clients.length == 0) {
      throw new IllegalStateException("No clients connected")
    }
    turn = new Turn(clients)
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
    clients.filter(_.clientId == clientId).head
  }

  val propertyChangeListener = new Object with PropertyChangeListener {
    def propertyChange[T](event : PropertyChange[T]) = {
      val msg = new PropertyChangeEvent(event.entity.asInstanceOf[ServerEntity].entityId, PropertyValue(event.name, event.newValue.asInstanceOf[Object]))
      sendToAll(msg)
    }
  }

  def addEntity(entity : ServerEntity) : Unit = {
    internalAddEntity(entity.entityId, entity)
    val controlledBy : ClientId = if (entity.isInstanceOf[ServerBeing]) entity.asInstanceOf[ServerBeing].controlledBy.clientId else null
    sendToAll(NewEntityEvent(entity.entityId, entity.entityTypeId, controlledBy, entity.getAllPropertyValues()))
    entity.addPropertyChangeListener(propertyChangeListener)
  }

  def sendToAll(event : Event) = {
    clients.foreach(_.eventListener.receive(event))
  }
}

trait ServerEntityContainer extends EntityContainer {
  type EntityBase = ServerEntity

  def findBeing(beingId : EntityId) : ServerBeing = {
    findEntity[ServerBeing](beingId)
  }

  def findItem(itemId : EntityId) : ServerItem = {
    findEntity[ServerItem](itemId)
  }

}

object App extends Application {
  println("hello world")
}
