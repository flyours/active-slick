package io.strongtyped.active.slick

import io.strongtyped.active.slick.test.H2Suite
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class CrudTest extends FlatSpec with H2Suite with JdbcProfileProvider {

  import jdbcProfile.api._

  behavior of "An EntityDao (CRUD)"

  it should "support all CRUD operations" in {
    rollback {
      for {
      // collect initial count
        initialCount <- Foos.count

        // save new entry
        savedEntry <- Foo("Foo").save()

        // count again, must be initialCount + 1
        count <- Foos.count

        // update entry
        updatedEntry <- savedEntry.copy(name = "Bar").save()

        // find it back from DB
        found <- Foos.findById(savedEntry.id.get)

        // delete it
        _ <- found.delete()

        // count total one more time
        finalCount <- Foos.count
      } yield {

        // check that we can add new entry
        count shouldBe (initialCount + 1)

        // check entity properties
        savedEntry.id shouldBe 'defined
        savedEntry.name shouldBe "Foo"

        // found entry must be a 'Bar'
        found.name shouldBe "Bar"

        // after delete finalCount must equal initialCount
        finalCount shouldBe initialCount

        savedEntry
      }
    }
  }

  override def createSchema = {
    Foos.createSchema
  }


  case class Foo(name: String, id: Option[Int] = None) extends Identifiable {

    type Id = Int
  }

  class FooDao extends EntityActions[Foo](jdbcProfile) {

    class FooTable(tag: Tag) extends jdbcProfile.api.Table[Foo](tag, "FOO_CRUD_TEST") {

      def name = column[String]("NAME")

      def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

      def * = (name, id.?) <>(Foo.tupled, Foo.unapply)
    }

    type EntityTable = FooTable
    val tableQuery = TableQuery[EntityTable]

    def $id(table: FooTable) = table.id

    val idLens = SimpleLens[Foo, Option[Int]](_.id, (entry, id) => entry.copy(id = id))

    def createSchema = {
      import jdbcProfile.api._
      tableQuery.schema.create
    }
  }

  val Foos = new FooDao


  implicit class EntryExtensions(val model: Foo) extends ActiveRecord[Foo] {

    val crudActions = Foos
  }

}
