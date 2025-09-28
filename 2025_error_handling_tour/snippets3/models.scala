package snippet3

//#region go
enum FileUploadError:
  case UnauthorizedUpload()
  case NotEnoughStorageQuota()
  case FileAlreadyExist()
//#endregion go

case class User(
  username: String,
)

case class Path()
case class FileBytes(size: Int)
case class File(path: String)
