import java.time.LocalDate

import EmployeeSpe.EmployeeId
import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.scalatest.DiffMatcher
import org.scalatest.{Matchers, WordSpec}
import EmployeeSpe._
import io.scalaland.chimney.dsl._

class EmployeeSpec extends WordSpec with Matchers with DiffMatcher {

  val employee = Employee(
    id = EmployeeId("id1"),
    firstName = "Hope",
    lastName = "Atkins",
    address = Address(streetNumber = "15", line1 = "Doe St", line2 = "", country = "United Kingdom", postCode = "3333"),
    dateOfBirth = LocalDate.parse("1985-04-01")
  )

  val employeeV2 = employee.into[EmployeeV2]
    .withFieldComputed(_.level, _ => 3)
    .transform

  val actual = employeeV2.copy(level = 0)

  "test match" in {
    actual should matchTo(employeeV2)
  }

  "test" in {
    actual shouldBe (employeeV2)
  }

}

object EmployeeSpe {
  import java.time.LocalDate
  final case class EmployeeId(str: String)
  final case class Address(streetNumber: String, line1: String, line2: String, country: String, postCode: String)

  final case class Employee(
    id: EmployeeId,
    firstName: String,
    lastName: String,
    address: Address,
    dateOfBirth: LocalDate,
  )

  final case class EmployeeV2(
    id: EmployeeId,
    firstName: String,
    lastName: String,
    address: Address,
    dateOfBirth: LocalDate,
    level: Int,
  )
}