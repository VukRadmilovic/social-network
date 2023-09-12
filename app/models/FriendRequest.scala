package models

import models.RequestStatus.RequestStatus
import play.api.libs.json.{Json, OFormat, Reads}

import java.time.LocalDateTime

case class FriendRequest(
    id: Long,
    sender: String,
    receiver: String,
    status: RequestStatus,
    created: LocalDateTime
)

object FriendRequest {
  implicit val jsonFormat: OFormat[FriendRequest] = Json.format[FriendRequest]

  def create(sender: String, receiver: String): FriendRequest = {
    FriendRequest(
      0,
      sender,
      receiver,
      RequestStatus.Pending,
      LocalDateTime.now
    )
  }
}

object RequestStatus extends Enumeration {
  type RequestStatus = Value
  val Pending, Accepted, Rejected = Value

  implicit val statusReads: Reads[models.RequestStatus.Value] =
    Reads.enumNameReads(RequestStatus)
}

object RequestResolution extends Enumeration {
  type RequestResolution = Value
  val Accept, Reject, Cancel = Value
}
