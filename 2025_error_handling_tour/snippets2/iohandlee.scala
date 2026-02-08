package snippet

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.*
import java.io.File
import snippet.*

object iohandlee {

  // format: off

  // #region go
  import iohandle.*

  def getQuotaForPath(user: User, path: Path)(implicit raise: IORaise[UnauthorizedUpload]): IO[Int] = ???
  def getFile(path: Path): IO[Option[File]] = ???
  def doUpload(path: Path, bytes: Array[Byte]): IO[Unit] = ???

  def upload(user: User, path: Path, bytes: Array[Byte]): IO[Either[FileUploadError, Unit]] =
    ioHandling[FileUploadError] { implicit handle: IORaise[FileUploadError] =>
      for {
        bytesQuota <- getQuotaForPath(user, path)
        _ <- if (bytesQuota - bytes.length < 0)
               ioAbort(NotEnoughStorageQuota())
             else IO.unit
        _ <- getFile(path).flatMap(maybeFile => ioAbortIf(maybeFile.nonEmpty, FileAlreadyExist()))
        _ <- doUpload(path, bytes)
      } yield ()
    }.toEither
  // #endregion go

}
