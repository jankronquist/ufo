package tinkerway.ufo.entity


import org.junit.Test

object ConstructionTesting {
  def main(args: Array[String]) {
    println(make(classOf[DummyEntity]))
  }
  def make[T <: Entity](cl : Class[T]) : T={
    val in = cl.getConstructor().newInstance()
    println(in.asInstanceOf[AbstractEntity].entityTypeId)
    in.asInstanceOf[T]
  }
}