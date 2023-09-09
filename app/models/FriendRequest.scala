package models

import models.RequestStatus.RequestStatus

import java.time.LocalDateTime
import play.api.libs.json.{Json, OFormat, Reads}

case class FriendRequest(
    id: Long,
    sender: String,
    receiver: String,
    status: RequestStatus,
    created: LocalDateTime,
    updated: Option[LocalDateTime]
)

object FriendRequest {
  implicit val jsonFormat: OFormat[FriendRequest] = Json.format[FriendRequest]

  def create(sender: String, receiver: String): FriendRequest = {
    FriendRequest(
      0,
      sender,
      receiver,
      RequestStatus.Pending,
      LocalDateTime.now(),
      None
    )
  }
}

object RequestStatus extends Enumeration {
  type RequestStatus = Value
  val Pending, Accepted, Rejected = Value

  implicit val statusReads: Reads[models.RequestStatus.Value] =
    Reads.enumNameReads(RequestStatus)
}
