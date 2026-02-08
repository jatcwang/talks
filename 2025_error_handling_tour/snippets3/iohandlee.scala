package snippet3.iohandlee

import snippet3.*
import FileUploadError.*
import cats.effect.IO
import cats.implicits.*

  // format: off

// #region go
import iohandle.*

def getQuotaForPath(user: User, path: Path)(using IORaise[UnauthorizedUpload]): IO[Int] = ???
def getFile(path: Path): IO[Option[File]] = ???
def doUpload(path: Path, bytes: Array[Byte]): IO[Unit] = ???

def upload(user: User, path: Path, bytes: Array[Byte]): IO[Either[FileUploadError, Unit]] =
    ioHandling[FileUploadError]:
      for
        bytesQuota <- getQuotaForPath(user, path)
        _ <- ioAbortIf(bytesQuota - bytes.length < 0, NotEnoughStorageQuota())
        _ <- getFile(path).abortIf(_.nonEmpty, FileAlreadyExist())
        _ <- doUpload(path, bytes)
      yield ()
    .toEither
// #endregion go
