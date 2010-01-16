package tinkerway.ufo.entity


import tinkerway.ufo.api._
import org.junit._
import Assert._
import org.mockito.Mockito._

class DummyEntity extends AbstractEntity {
  object firstName extends Property[String]("Test") 
  val entityTypeId : EntityTypeId = null
  val entityId : EntityId = null
  val entityType = classOf[DummyEntity]
  override def getAllProperties() = super.getAllProperties()
}

object Dummy extends Application {
  org.junit.runner.JUnitCore.main("tinkerway.ufo.entity.EntityTest")
}

@Test
class EntityTest {
    @Ignore
    @Test
    def whenPropertyIsChangedGeneratePropertyChangeEvent() = {
      val entity = new DummyEntity()
      val capture = new CapturePropertyChangeListener()
      entity.addPropertyChangeListener(capture)
      entity.firstName := "Changed"
      val event = capture.take()
      assertEquals("firstName", event.name)
      assertEquals("Changed", event.newValue)
      assertEquals("Test", event.oldValue)
    }

  @Test
  def whenEntityIsCreatedItShouldContainAllProperties() = {
    val entity = new DummyEntity()
    assertEquals(List("Test"), entity.getAllProperties().filter(_.name.equals("firstName")).map(_.apply()))
  }

}
