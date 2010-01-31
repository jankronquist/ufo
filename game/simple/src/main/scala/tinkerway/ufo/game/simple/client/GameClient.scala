package tinkerway.ufo.game.simple.client

import tinkerway.ufo.api._
import java.util.concurrent.CountDownLatch
import collection.jcl.ArrayList
import tinkerway.ufo.game.simple.domain.Domain._
import java.io.File
import org.newdawn.slick._
import tinkerway.ufo.client.common._
import tinkerway.ufo.entity.EntityListener
import tinkerway.ufo.domain.{HasLocation, Being, Item, HasPosition}

class TheClientApp(server : ServerConnector) {

  val startSignal = new CountDownLatch(1);
  new Thread(new Runnable() {
    def run() = {
      val app = new AppGameContainer(new SimpleClientUI(startSignal, server));
      app.setDisplayMode(800, 600, false);
      app.start();
    }
  }).start()
  startSignal.await()
}


trait Sprite extends GameConstants {
  this : MightHavePosition =>
  val image : Image
  def render(gc: GameContainer, g: Graphics) = {
    getPosition() match {
      case Some(pos) => image.draw(pos.x*tileWidth, pos.y*tileHeight)
      case _ => // do nothing
    }
  }
}

trait ClientDomain {
  this : SpriteSheetContainer =>
  
  trait ClientHumanBeing extends HumanBeing with Sprite {
    val sx = (Math.random*4).asInstanceOf[Int]
    val sy = (Math.random*4).asInstanceOf[Int]
    val image = beings.getSprite(sx, sy)
  }

  trait ClientHealingPotion extends HealingPotion with Sprite {
    val image = misc.getSprite(2, 2)
  }

  trait ClientGun extends Gun with Sprite {
    val image = misc.getSprite(3, 2)
  }
}

trait GameConstants {
  val tileWidth = 32
  val tileHeight = 32
}

trait SpriteSheetContainer {
  var tiles : SpriteSheet
  var beings : SpriteSheet
  var misc : SpriteSheet
}


class SimpleClientUI(startSignal : CountDownLatch, server : ServerConnector) extends BasicGame("SimpleGame") with GameConstants with ClientDomain with SpriteSheetContainer {
  var x : Int = 0
  var y : Int = 0
  var scale : Float = 1
  var world : List[Int] = Nil
  var selectedBeing : ClientEntity = null
  var entityIterator : Iterator[ClientEntity] = null
  // TODO: these should really be val
  var client : SimpleClient = null
  var actionHandler : ActionHandler = null

  var tiles : SpriteSheet = null
  var beings : SpriteSheet = null
  var misc : SpriteSheet = null


  def loadImage(name : String) = {
    new Image(Thread.currentThread.getContextClassLoader.getResourceAsStream(name), name, false)
  }

  override def init(gc: GameContainer) = {
    tiles = new SpriteSheet(loadImage("ground/dg_dungeon32.gif"), tileWidth, tileHeight)
    beings = new SpriteSheet(loadImage("beings/dg_humans32.gif"), tileWidth, tileHeight)
    misc = new SpriteSheet(loadImage("items/dg_misc32.gif"), tileWidth, tileHeight)
    val entityTypes = new FunctionEntityTypeContainer
    entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with ClientHumanBeing)
    entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with ClientHealingPotion)
    entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with ClientGun)
    client = new SimpleClient(entityTypes)
    actionHandler = server.connect(client)

    val hasLocation = new ClientEntity(EntityId(-1)) with ClientHealingPotion

    // TODO: how to handle the case when an item is removed? (it need to be removed from the inventory as well)
    client.onPropertyChange(hasLocation.location, (entity : ClientEntity, from : Location, to : Location) => {
      println("Location has changed!")
      from match {
        case EntityLocation(entityId : EntityId) => client.findEntity(entityId).removeEntity(entity)
        case _ =>
      }
      to match {
        case EntityLocation(parentEntity : ClientEntity) => parentEntity.addEntity(entity)
        case _ =>
      }
    })
    client.addEntityListener(ClientEntityListener)
    

    startSignal.countDown()
    actionHandler.perform(BeginGame())
    gc.setVSync(true)
  }

  object ClientEntityListener extends EntityListener[ClientEntity] {
    def entityAdded(entity : ClientEntity) = {
    }
    def entityRemoved(entity : ClientEntity) = {
      if (entity.isInstanceOf[HasLocation]) {
        entity.asInstanceOf[HasLocation].location() match {
          case EntityLocation(entityId : EntityId) => client.findEntity(entityId).removeEntity(entity)
          case _ =>
        }
      }
    }
  }

  override def update(gc: GameContainer, delta: Int) = {
    val input = gc.getInput();

    if(input.isKeyDown(Input.KEY_A)) {
      moveEntity(-1, 0)
    } else if(input.isKeyDown(Input.KEY_D)) {
      moveEntity(1, 0)
    } else if(input.isKeyDown(Input.KEY_W)) {
      moveEntity(0, -1)
    } else if(input.isKeyDown(Input.KEY_S)) {
      moveEntity(0, 1)
    } else if(input.isKeyDown(Input.KEY_J)) {
      pickUpItem(-1, 0)
    } else if(input.isKeyDown(Input.KEY_U)) {
      useItem()
    } else if(input.isKeyDown(Input.KEY_E)) {
      endTurn()
    } else if(input.isKeyDown(Input.KEY_SPACE)) {
      var nextBeing : ClientEntity = null
      while (nextBeing == null) {
        if (entityIterator == null || entityIterator.hasNext == false) {
          entityIterator = client.getAllEntities().filter(e => e.entity.isInstanceOf[Being] && client.clientId.equals(e.entity.asInstanceOf[Being].controlledBy()))
        }
        nextBeing = entityIterator.next()
      }
      selectedBeing = nextBeing
      println("SELECTED: " + selectedBeing)
      Thread.sleep(50)
    }

  }

  def moveEntity(xdiff : Int, ydiff : Int) = {
    if (selectedBeing != null) {
      val pos = selectedBeing.asInstanceOf[HasPosition].position()
      val dest = Position(pos.x+xdiff, pos.y+ydiff)
      actionHandler.perform(new Move(selectedBeing, dest))
    }
  }
  def pickUpItem(xdiff : Int, ydiff : Int) = {
    if (selectedBeing != null) {
      val pos = selectedBeing.asInstanceOf[HasPosition].position()
      val targetPosition = Position(pos.x+xdiff, pos.y+ydiff)
      client.findEntity(targetPosition) match {
        case Some(item : Item) => actionHandler.perform(new ItemAction(selectedBeing, Place(), item, new PositionLocation(targetPosition)))
        case _ => println("No item to pick up!")
      }
    }
  }

  // TODO: should include which item to use
  def useItem() = {
    if (selectedBeing != null) {
      selectedBeing.getItem() match {
        case Some(item : Item) => actionHandler.perform(new ItemAction(selectedBeing, Use(), item, new EntityLocation(selectedBeing)))
        case _ => println("No item to use!!!")
      }
    }
  }

  def endTurn() = {
    actionHandler.perform(EndTurn())
  }

  override def render(gc: GameContainer, g: Graphics) = {
    client.getAllEntities().foreach(e  => {
      if (e.isInstanceOf[Sprite]) {
        val sprite = e.asInstanceOf[Sprite]
        sprite.render(gc, g)
      }
    })
  }

}
