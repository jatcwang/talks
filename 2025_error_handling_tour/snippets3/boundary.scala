package snippet3
import FileUploadError.*
//#region go
import scala.util.boundary
import scala.util.boundary.{Label, break}

def getQuotaForPath(user: User, path: Path)(using Label[UnauthorizedUpload]): Int = ???
def getFile(path: Path): Option[File] = ???
def doUpload(path: Path, bytes: Array[Byte]): Unit = ???

def upload(user: User, path: Path, bytes: Array[Byte]): FileUploadError | Unit =
  boundary[FileUploadError | Unit]:
    val bytesQuota = getQuotaForPath(user, path)
    if bytesQuota - bytes.length < 0 then break(NotEnoughStorageQuota())
    if getFile(path).nonEmpty then break(FileAlreadyExist())
    doUpload(path, bytes)
//#endregion go
