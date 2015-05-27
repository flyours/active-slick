package io.strongtyped.active.slick.docexamples

import io.strongtyped.active.slick.{SimpleLens, ActiveSlick}
import io.strongtyped.active.slick.models.Identifiable
import slick.driver.{H2Driver, JdbcDriver}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait MappingActiveSlickIdentifiable {
  this: ActiveSlick =>

  import profile.api._

  case class Foo(name: String, id: Option[Int] = None) extends Identifiable {
    override type Id = Int
  }

  class FooTable(tag: Tag) extends EntityTable[Foo](tag, "FOOS") {
    def name = column[String]("NAME")

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def * = (name, id.?) <>(Foo.tupled, Foo.unapply)
  }

  val Foos = EntityTableQuery[Foo, FooTable](
    cons = tag => new FooTable(tag),
    idLens = SimpleLens[Foo, Option[Int]](_.id, (foo, id) => foo.copy(id = id))
  )

}

object MappingActiveSlickIdentifiableApp {

  class Components(override val profile: JdbcDriver) extends ActiveSlick with MappingActiveSlickIdentifiable {

    import profile.api._

    def run[T](block: Database => T): T = {
      val db = Database.forURL("jdbc:h2:mem:active-slick", driver = "org.h2.Driver")
      try {
        Await.ready(db.run(Foos.schema.create), 200 millis)
        block(db)
      } finally {
        db.close()
      }
    }
  }

  object Components {
    val instance = new Components(H2Driver)
  }

  import io.strongtyped.active.slick.docexamples.MappingActiveSlickIdentifiableApp.Components.instance._

  def main(args: Array[String]): Unit = {
    run { db =>
      val foo = Foo("foo")
      val fooWithId = Await.result(db.run(Foos.save(foo)), 200 millis)
      assert(fooWithId.id.isDefined, "Foo's ID should be defined")
    }
  }
}
