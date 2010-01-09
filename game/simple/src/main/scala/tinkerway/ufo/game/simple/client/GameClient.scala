package tinkerway.ufo.game.simple.client

import api._
import java.util.concurrent.CountDownLatch
import collection.jcl.ArrayList
import domain.Domain._
import java.io.File
import org.newdawn.slick._
import ufo.client.common._
import ufo.domain.{Item, HasPosition}
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

class ClientHumanBeing extends ClientEntity with HumanBeing with Sprite {
  val sx = (Math.random*4).asInstanceOf[Int]
  val sy = (Math.random*4).asInstanceOf[Int]
  val image = GlobalData.beings.getSprite(sx, sy)
}

class ClientHealingPotion extends ClientEntity with HealingPotion with Sprite {
  val image = GlobalData.misc.getSprite(2, 2)
}

trait GameConstants {
  val tileWidth = 32
  val tileHeight = 32
}

object GlobalData {
  var tiles : SpriteSheet = null
  var beings : SpriteSheet = null
  var misc : SpriteSheet = null
}

class SimpleClientUI(startSignal : CountDownLatch, server : ServerConnector) extends BasicGame("SimpleGame") with GameConstants {
  var x : Int = 0
  var y : Int = 0
  var scale : Float = 1
  var world : List[Int] = Nil
  var selectedBeing : SimpleEntity = null
  var entityIterator : Iterator[SimpleEntity] = null
  // TODO: these should really be val
  var client : SimpleClient = null
  var actionHandler : ActionHandler = null

  def loadImage(name : String) = {
    new Image(Thread.currentThread.getContextClassLoader.getResourceAsStream(name), name, false)
  }

  override def init(gc: GameContainer) = {
    GlobalData.tiles = new SpriteSheet(loadImage("ground/dg_dungeon32.gif"), tileWidth, tileHeight)
    GlobalData.beings = new SpriteSheet(loadImage("beings/dg_humans32.gif"), tileWidth, tileHeight)
    GlobalData.misc = new SpriteSheet(loadImage("items/dg_misc32.gif"), tileWidth, tileHeight)
    val entityTypes = new FunctionEntityTypeContainer
    entityTypes.registerEntityType(() => new ClientHumanBeing())
    entityTypes.registerEntityType(() => new ClientHealingPotion())
//    entityTypes.registerEntityType(classOf[ClientHumanBeing])
//    entityTypes.registerEntityType(classOf[ClientHealingPotion])
    client = new SimpleClient(entityTypes)
    actionHandler = server.connect(client)
    startSignal.countDown()
    actionHandler.perform(BeginGame())
    gc.setVSync(true)
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
    } else if(input.isKeyDown(Input.KEY_SPACE)) {
      var nextBeing : Option[SimpleEntity] = None
      while (nextBeing == None) {
        if (entityIterator == null || entityIterator.hasNext == false) {
          entityIterator = client.entities.values
        }
        nextBeing = entityIterator.find(e => client.clientId.equals(e.controlledBy))
      }
      selectedBeing = nextBeing.get
      println("SELECTED: " + selectedBeing)
      Thread.sleep(50)
    }

  }

  def moveEntity(xdiff : Int, ydiff : Int) = {
    if (selectedBeing != null) {
      val pos = selectedBeing.entity.asInstanceOf[HasPosition].position()
      val dest = Position(pos.x+xdiff, pos.y+ydiff)
      val result = actionHandler.perform(new Move(selectedBeing.entityId, dest))
      println(result)
    }
  }

  override def render(gc: GameContainer, g: Graphics) = {
    client.entities.values.foreach(e  => {
      if (e.entity.isInstanceOf[Sprite]) {
        val sprite = e.entity.asInstanceOf[Sprite]
        sprite.render(gc, g)
      }
    })
  }

}
