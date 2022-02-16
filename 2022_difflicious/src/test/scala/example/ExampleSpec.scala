package example
import org.scalatest.freespec.AnyFreeSpec

class ExampleSpec extends AnyFreeSpec {
  "test fail" in {
    fail("simulated test failure")
  }

  "test succeed" in {
    succeed
  }
}
