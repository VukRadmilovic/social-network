package repositories

import models.RequestStatus.{Pending, RequestStatus}
import models.{FriendRequest, RequestStatus}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FriendRequestRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val friendRequestTable = TableQuery[FriendRequestTable]

  implicit val requestStatusColumnType: BaseColumnType[RequestStatus] =
    MappedColumnType.base[RequestStatus, String](
      e => e.toString,
      s => RequestStatus.withName(s)
    )

  def send(friendRequest: FriendRequest): Future[FriendRequest] = {
    db.run(friendRequestTable += friendRequest)
      .map(_ => friendRequest)
  }

  def getBySender(sender: String): Future[Seq[FriendRequest]] = {
    db.run(friendRequestTable.filter(_.sender === sender).result)
  }

  def getByReceiver(receiver: String): Future[Seq[FriendRequest]] = {
    db.run(friendRequestTable.filter(_.receiver === receiver).result)
  }

  def pendingRequestFromSenderExists(newRequest: FriendRequest): Future[Boolean] = {
    db.run(
      friendRequestTable
        .filter(existingRequest =>
          existingRequest.sender === newRequest.sender &&
            existingRequest.receiver === newRequest.receiver &&
            existingRequest.status === Pending
        )
        .exists
        .result
    )
  }

  def pendingRequestToSenderExists(newRequest: FriendRequest): Future[Boolean] = {
    db.run(
      friendRequestTable
        .filter(existingRequest =>
          existingRequest.sender === newRequest.receiver &&
            existingRequest.receiver === newRequest.sender &&
            existingRequest.status === Pending
        )
        .exists
        .result
    )
  }

  def update(friendRequest: FriendRequest): Future[FriendRequest] = {
    db.run(
      friendRequestTable.filter(_.id === friendRequest.id).update(friendRequest)
    ).map(_ => friendRequest)
  }

  def delete(id: Long): Future[Unit] = {
    db.run(friendRequestTable.filter(_.id === id).delete)
      .map(_ => ())
  }

  class FriendRequestTable(tag: Tag)
      extends Table[FriendRequest](tag, "friend_requests") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender: Rep[String] = column[String]("sender")
    def receiver: Rep[String] = column[String]("receiver")
    def status: Rep[RequestStatus] = column[RequestStatus]("status")
    def created: Rep[LocalDateTime] = column[LocalDateTime]("created")
    def updated: Rep[Option[LocalDateTime]] =
      column[Option[LocalDateTime]]("updated")

    override def * : ProvenShape[FriendRequest] =
      (id, sender, receiver, status, created, updated) <>
        ((FriendRequest.apply _).tupled, FriendRequest.unapply)
  }
}
