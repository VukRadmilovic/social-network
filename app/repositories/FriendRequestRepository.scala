package repositories

import dtos.PaginatedResult
import models.RequestStatus.{Pending, RequestStatus}
import models.{FriendRequest, RequestStatus}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FriendRequestRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  import profile.api._

  private val friendRequestTable = TableQuery[FriendRequestTable]

  implicit val requestStatusColumnType: BaseColumnType[RequestStatus] =
    MappedColumnType.base[RequestStatus, String](
      e => e.toString,
      s => RequestStatus.withName(s)
    )

  def send(friendRequest: FriendRequest): Future[Unit] = {
    db.run(friendRequestTable += friendRequest).map(_ => ())
  }

  def getBySender(sender: String, limit: Long, page: Long): Future[PaginatedResult[FriendRequest]] = db.run {
    val offset = page * limit
    val query = friendRequestTable.filter(_.sender === sender)

    for {
      requests <- query.drop(offset).take(limit).result
      numberOfRequests <- query.length.result
    } yield PaginatedResult(
      totalCount = numberOfRequests,
      entries = requests.toList,
      hasNextPage = offset + limit < numberOfRequests
    )
  }

  def getByReceiver(receiver: String, limit: Long, page: Long): Future[PaginatedResult[FriendRequest]] = db.run {
    val offset = page * limit
    val query = friendRequestTable.filter(_.receiver === receiver)

    for {
      requests <- query.drop(offset).take(limit).result
      numberOfRequests <- query.length.result
    } yield PaginatedResult(
      totalCount = numberOfRequests,
      entries = requests.toList,
      hasNextPage = offset + limit < numberOfRequests
    )
  }

  def getFriendRequestById(id: Long): Future[Option[FriendRequest]] = {
    db.run(friendRequestTable.filter(_.id === id).result.headOption)
  }

  private def pendingRequestExists(sender: String, receiver: String): Future[Boolean] = {
    db.run(
      friendRequestTable
        .filter(existingRequest =>
          existingRequest.sender === sender &&
            existingRequest.receiver === receiver &&
            existingRequest.status === Pending
        )
        .exists
        .result
    )
  }

  def pendingRequestFromSenderExists(newRequest: FriendRequest): Future[Boolean] = {
    pendingRequestExists(newRequest.sender, newRequest.receiver)
  }

  def pendingRequestToSenderExists(newRequest: FriendRequest): Future[Boolean] = {
    pendingRequestExists(newRequest.receiver, newRequest.sender)
  }

  private def update(id: Long, status: RequestStatus): Future[Unit] = {
    db.run(friendRequestTable.filter(_.id === id).map(_.status).update(status)).map(_ => ())
  }

  def reject(id: Long): Future[Unit] = {
    update(id, RequestStatus.Rejected)
  }

  def accept(id: Long): Future[Unit] = {
    update(id, RequestStatus.Accepted)
  }

  def cancel(id: Long): Future[Unit] = {
    db.run(friendRequestTable.filter(_.id === id).delete).map(_ => ())
  }

  private class FriendRequestTable(tag: Tag)
      extends Table[FriendRequest](tag, "friend_requests") {

    implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
      ldt => ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
      str => LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    )

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender: Rep[String] = column[String]("sender")
    def receiver: Rep[String] = column[String]("receiver")
    def status: Rep[RequestStatus] = column[RequestStatus]("status")
    private def created: Rep[LocalDateTime] = column[LocalDateTime]("created")

    override def * : ProvenShape[FriendRequest] =
      (id, sender, receiver, status, created) <>
        ((FriendRequest.apply _).tupled, FriendRequest.unapply)
  }
}
