package tinkerway.ufo.client.common



import tinkerway.ufo.api._
import tinkerway.ufo.domain.{HasPosition, Human}
import org.junit._
import Assert._
import tinkerway.ufo.entity.{PropertyContainer, Property}

trait DummyEventHandler {
  this : PropertyContainer with HasPosition =>

  var dummy : Position = null

  onChange(position, (from : Position, to : Position) => {
    println("onPositionChange: to=" + to)
    dummy = to
  })

}

class ClientHuman extends ClientEntity with Human with DummyEventHandler with HasPosition

class PropertyCaptureHandler {
  var result : Any = null
  var count = 0
  def register[T](property : Property[T], client : SimpleClient) {
    client.onPropertyChange(property, (entity : ClientEntity, from : T, to : T) => {
      println("global onPositionChange: to=" + to)
      count = count+1
      result = to
    })
    
  }
}

class ClientTest {
  @Before
  def before() = {
  }

  @Test
  def whenPropertyChangeEventIsCalledThenEventHandlerMethodsShouldBeInvoked() = {
    val entityTypes = new ClassEntityTypeContainer
    entityTypes.registerEntityType(classOf[ClientHuman])
    val client = new SimpleClient(entityTypes)

    val human = new ClientHuman
    val globalPositionCapture = new PropertyCaptureHandler()
    globalPositionCapture.register(human.position, client)

    val entityId1 = new EntityId(1)
    val entityId2 = new EntityId(2)
    val entityType = human.entityTypeId
    val controlledBy = new ClientId(1)
    val properties : List[PropertyValue] = Nil
    val newPosition = new Position(1, 2)

    client.receive(new NewEntityEvent(entityId1, entityType, controlledBy, properties))
    client.receive(new NewEntityEvent(entityId2, entityType, controlledBy, properties))
    client.receive(new PropertyChangeEvent(entityId1, new PropertyValue("position", newPosition)))
    
    assertNull(human.dummy)
    assertEquals(newPosition, client.getEntity(entityId1).asInstanceOf[DummyEventHandler].dummy)
    assertEquals(newPosition, globalPositionCapture.result)
    assertEquals(1, globalPositionCapture.count)
    assertNull(client.getEntity(entityId2).asInstanceOf[DummyEventHandler].dummy)
    assertEquals(newPosition, client.getEntity(entityId1).asInstanceOf[Human].position())
    assertNull(human.position())
    assertNull(client.getEntity(entityId2).asInstanceOf[Human].position())
  }

}

