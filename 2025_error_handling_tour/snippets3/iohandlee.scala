package snippet3.iohandlee

import snippet3.*
import FileUploadError.*
import cats.effect.IO
import cats.implicits.*

  // format: off

import iohandle.*

def getFile(path: Path): IO[Option[File]] = ???
def doUpload(path: Path, bytes: Array[Byte]): IO[Unit] = ???
// #region go
def getQuotaForPath(user: User, path: Path)(using IORaise[UnauthorizedUpload]): IO[Int] = ???

def upload(user: User, path: Path, bytes: Array[Byte]): IO[String] =
    ioHandling[FileUploadError]:
      for
        bytesQuota <- getQuotaForPath(user, path)
        _ <- if bytesQuota - bytes.length < 0
             then ioAbort(NotEnoughStorageQuota())
             else IO.unit
        _ <- getFile(path).flatMap(maybeFile => ioAbortIf(maybeFile.nonEmpty, FileAlreadyExist()))
        _ <- doUpload(path, bytes)
      yield "uploaded!"
    .rescue:
      case _: UnauthorizedUpload => "unauthorized!"
      case _: FileAlreadyExist => "file already exist!"
      case _: NotEnoughStorageQuota => "out of quota!"
// #endregion go
