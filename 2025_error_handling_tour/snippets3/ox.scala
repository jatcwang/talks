package snippet3.oxx
import snippet3.*
import FileUploadError.*
//#region go
import ox.either
import ox.either.{fail, ok}

def getQuotaForPath(user: User, path: Path): Either[UnauthorizedUpload, Int] = ???
def getFile(path: Path): Option[File] = ???
def doUpload(path: Path, bytes: Array[Byte]): Unit = ???

def upload(user: User, path: Path, bytes: Array[Byte]): Either[FileUploadError, Unit] =
  either[FileUploadError, Unit]:
    val bytesQuota = getQuotaForPath(user, path).ok()
    if bytesQuota - bytes.length < 0 then NotEnoughStorageQuota().fail()
    if getFile(path).nonEmpty then FileAlreadyExist().fail()
    doUpload(path, bytes)

//#endregion go
