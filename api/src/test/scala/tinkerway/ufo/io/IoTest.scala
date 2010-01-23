package tinkerway.ufo.io

import tinkerway.ufo.api.{EntityTypeId, EntityId, Position}
import tinkerway.ufo.entity._
import org.junit.Assert._
import org.junit.Test


class IoTest {
  class SimpleEntity(val entityId : EntityId) extends AbstractEntity {
    object name extends Property[String]("Test")
    val entityTypeId : EntityTypeId = EntityTypeId(1)
    val entityType = classOf[DummyEntity]
    override def getAllProperties() = super.getAllProperties()
  }


  class SimpleEntityContainer extends EntityContainer {
    type EntityBase = SimpleEntity

  }

  case class SomeMessage(name:String, entity : SimpleEntity, age: Int)


  @Test
  def serializationTest() = {
    val being = new SimpleEntity(EntityId(123))
    being.name := "dummy"
    val container = new SimpleEntityContainer
    container.internalAddEntity(being.entityId, being)
    val xstream = new EntityXStream(container)
    val xml = xstream.toXML(SomeMessage("qwe", being, 35))
    val res = xstream.fromXML(xml).asInstanceOf[SomeMessage]
    assertEquals("qwe", res.name)
    assertEquals(35, res.age)
    assertTrue(being == res.entity)
  }

}