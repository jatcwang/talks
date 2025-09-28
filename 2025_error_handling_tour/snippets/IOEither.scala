package snippet.ioeither
import cats.effect.IO
import java.io.File

def getQuotaForPath(user: User, path: Path): IO[Either[UnauthorizedUpload, Int]] =
  IO.pure(Right(100))

def getFile(path: Path): IO[Option[File]] = {
  IO.pure(Some(new File("some_file.txt")))
}

def doUpload(path: Path, bytes: Array[Byte]): IO[Unit] = {
  IO.unit
}

def upload(user: User, path: Path, bytes: Array[Byte]): IO[Either[FileUploadError, Unit]] =
  getQuotaForPath(user, path).flatMap {
    case Left(e) => IO.pure(Left(e))
    case Right(bytesQuota) =>
      if (bytesQuota - bytes.length < 0) IO.pure(Left(NotEnoughStorageQuota()))
      else
        getFile(path).flatMap {
          case Some(f) => IO.pure(Left(FileAlreadyExist()))
          case None    => doUpload(path, bytes).map(_ => Right(()))
        }
  }
