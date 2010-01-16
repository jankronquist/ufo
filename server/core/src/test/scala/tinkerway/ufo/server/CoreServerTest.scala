package tinkerway.ufo.server

import tinkerway.ufo.api._
import tinkerway.ufo.client.common.{FunctionEntityTypeContainer, SimpleClient, ClientEntity}
import tinkerway.ufo.domain.{HasPosition, Human}
import tinkerway.ufo.entity._
import java.lang.reflect.{Method, InvocationHandler, Proxy}
import org.junit.Assert._
import org.junit.{Ignore, Test, Before}
import scala.collection.mutable.HashMap


trait ClientHuman extends Human

class CoreServerTest {
  val width = 5
  val height = 5
  val server = new Server(new World(Size(width, height)))
  val entityTypes = new FunctionEntityTypeContainer
  entityTypes.registerEntityType((entityId :EntityId) => new ClientEntity(entityId) with ClientHuman)
  val client = new SimpleClient(entityTypes)
  val actionHandler = server.connect(client)
  
  @Before
  def before() = {
    performSuccessful(BeginGame())
  }

  @Test
  def notBeAbleToStartAgain() = {
    assertFalse(actionHandler.perform(BeginGame()).equals(Successful()))
  }

  @Test
  def whenAddingAnEntitySendNewEntityEvent() = {
    val being1 = IdFactory.makeEntityId()
    server.addEntity(new ServerBeing(being1, Position(1, 1), server.getClient(client.clientId)) with Human)
    assertTrue(client.findEntity(being1) != null)
  }

  @Test
  def whenMovingThePositionShouldChangeOnTheClient() = {
    val being1 = IdFactory.makeEntityId()
    val serverBeing = new ServerBeing(being1, Position(1, 1), server.getClient(client.clientId)) with Human
    server.addEntity(serverBeing)

    val targetPosition = Position(1, 2)
    performSuccessful(Move(being1, targetPosition))
    val clientPosition = client.findEntity(being1).asInstanceOf[HasPosition].position()
    assertEquals(targetPosition, clientPosition)
  }

  @Test
  def addEntityWithExistingIdShouldNotBePossible() = {
    val being1 = IdFactory.makeEntityId()
    server.addEntity(new ServerBeing(being1, Position(2, 2), server.getClient(client.clientId)) with Human)
    try {
      server.addEntity(new ServerBeing(being1, Position(2, 3), server.getClient(client.clientId)) with Human)
      fail("expected illegalargument")
    } catch {
      case e : IllegalArgumentException => // ok
      case e : Throwable => fail("expected illegalargument")
    }
  }

  @Test
  def movingMultipleStepsShouldNotBePossible() = {
    val being1 = IdFactory.makeEntityId()
    server.addEntity(new ServerBeing(being1, Position(2, 2), server.getClient(client.clientId)) with Human)

    unsuccessful(Move(being1, Position(3, 3)))
    unsuccessful(Move(being1, Position(1, 1)))
    unsuccessful(Move(being1, Position(2, 4)))
    unsuccessful(Move(being1, Position(0, 2)))
  }
  
  @Test
  def passingThroughEntityShouldNotBePossible() = {
    val being1 = IdFactory.makeEntityId()
    val being2 = IdFactory.makeEntityId()
    server.addEntity(new ServerBeing(being1, Position(2, 2), server.getClient(client.clientId)) with Human)
    server.addEntity(new ServerBeing(being2, Position(2, 3), server.getClient(client.clientId)) with Human)

    unsuccessful(Move(being1, Position(2, 3)))
  }

  @Test
  def atTopLeftMovingLeftOrUpShouldNotBePossible() = {
    val being1 = IdFactory.makeEntityId()
    val serverBeing = new ServerBeing(being1, Position(0, 0), server.getClient(client.clientId)) with Human
    server.addEntity(serverBeing)

    unsuccessful(Move(being1, Position(-1, 0)))
    unsuccessful(Move(being1, Position(0, -1)))
  }
  
  @Test
  def atBottomRightMovingRightOrDownShouldNotBePossible() = {
    val being1 = IdFactory.makeEntityId()
    val serverBeing = new ServerBeing(being1, Position(width-1, height-1), server.getClient(client.clientId)) with Human
    server.addEntity(serverBeing)

    unsuccessful(Move(being1, Position(-1, 0)))
    unsuccessful(Move(being1, Position(0, -1)))
  }
  
  private def performSuccessful(action : Action) = {
    assertEquals(Successful(), actionHandler.perform(action))
  }

  private def unsuccessful(action : Action) = {
    val result = actionHandler.perform(action)
    assertEquals(IllegalAction(), result)
  }
}

object ClientEventHandler extends PropertyContainer with HasPosition {

  val entityTypeId : EntityTypeId = null

  def init(client : SimpleClient) = {
    def animatePosition(entity : ClientEntity, from : Position, to : Position) = {
      println("ANIMATE! from=" + from + " to=" + to)
    }

    client.onPropertyChange(position, animatePosition)
  }
}