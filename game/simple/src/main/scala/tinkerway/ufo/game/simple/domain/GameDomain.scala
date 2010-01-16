package tinkerway.ufo.game.simple.domain


import tinkerway.ufo.api.EntityTypeId
import tinkerway.ufo.domain.{Item, Being}
object Domain {

  trait HumanBeing extends Being {
    val entityTypeId = new EntityTypeId(1)
  }

  trait HealingPotion extends Item {
    val entityTypeId = new EntityTypeId(2)
  }

}