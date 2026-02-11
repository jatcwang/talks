package snippet3.screenn

import cats.effect.unsafe.implicits.global 
import snippet3.User
import iohandle.ioscreen.*
import iohandle.*
import iohandle.IOScreen
import cats.effect.IO
import cats.data.{Ior, NonEmptyVector}

def validateUser(user: User)(ioScreen: IOScreen[InvalidUserError]): IO[User] = {
  val len = user.username.split(" ").length
  for 
    _ <- if len < 1 then ioScreen.report(InvalidUserError.MissingLastName) else IO.unit
    _ <- if len < 2 then ioScreen.reportAndReject(InvalidUserError.MissingFirstName) else IO.unit
  yield user
}

enum InvalidUserError:
  case MissingFirstName
  case MissingLastName

def validateUsers(users: List[User]): IO[Ior[NonEmptyVector[InvalidUserError], List[User]]] =
  ioScreening[InvalidUserError]: handle =>
    handle.parTraverseScreening(users)(validateUser)
  .toIor

@main
def mainScreen = { 
  val res = validateUsers(List(User("John doe"), User("john"), User("john john john")))
  .unsafeRunSync()
  println(res)
}


//def validateUsers(user1: User, user2: User): IO[Ior[NonEmptyVector[InvalidUserError], (User, User)]] =
//  ioScreening[InvalidUserError]: handle =>
//    handle.parZipScreening(validateUser(user1), validateUser(user2))
//  .toIor
  
  
