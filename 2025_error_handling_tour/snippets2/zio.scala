package snippet

import zio._
import java.io.File
import snippet._

object zioo {

  // #region go
  def getQuotaForPath(user: User, path: Path): ZIO[Any, UnauthorizedUpload, Int] = ???
  // UIO is an alias for ZIO[Any, Nothing, A]
  def getFile(path: Path): UIO[Option[File]] = ???
  def doUpload(path: Path, bytes: Array[Byte]): UIO[Unit] = ???

  def upload(user: User, path: Path, bytes: Array[Byte]): ZIO[Any, FileUploadError, Unit] = {
    for {
      bytesQuota <- getQuotaForPath(user, path)
      _ <- ZIO.cond(bytesQuota - bytes.length >= 0, (), NotEnoughStorageQuota())
      _ <- getFile(path).filterOrFail(_.isEmpty)(FileAlreadyExist())
      _ <- doUpload(path, bytes)
    } yield ()
  }
  // #endregion go

}
