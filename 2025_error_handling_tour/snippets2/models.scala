package snippet

//#region go
sealed trait FileUploadError

case class UnauthorizedUpload() extends FileUploadError
case class NotEnoughStorageQuota() extends FileUploadError
case class FileAlreadyExist() extends FileUploadError
//#endregion go

case class User(
  username: String,
)

case class Path()
case class FileBytes(size: Int)
case class File()
