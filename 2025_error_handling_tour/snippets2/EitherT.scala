package snippet

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.*
import java.io.File
import snippet.*

object eithert {

  def getFile(path: Path): IO[Option[File]] = ???
  def doUpload(path: Path, bytes: Array[Byte]): IO[Unit] = ???

  // #region go
  def getQuotaForPath(user: User, path: Path): IO[Either[UnauthorizedUpload, Int]] = ???

  def upload(user: User, path: Path, bytes: Array[Byte]): IO[Either[FileUploadError, Unit]] = {
    (for {
      bytesQuota <- EitherT(getQuotaForPath(user, path))
      _ <- EitherT.cond[IO](bytesQuota - bytes.length >= 0, (), NotEnoughStorageQuota())
      _ <- EitherT(
        getFile(path).flatMap {
          case None    => IO(Right(()))
          case Some(_) => IO(Left(FileAlreadyExist()))
        },
      )
      _ <- EitherT.liftF[IO, FileUploadError, Unit](doUpload(path, bytes))
    } yield ()).value
  }
  // #endregion go

}
