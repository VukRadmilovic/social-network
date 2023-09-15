package dtos

import models.Post
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class OutputPostDTO (id: Long,
                          poster: String,
                          content: String,
                          posted: LocalDateTime,
                          likes: Long,
                          liked: Boolean)

object OutputPostDTO {
  implicit val jsonFormat: OFormat[OutputPostDTO] = Json.format[OutputPostDTO]

  def apply(post: Post, liked: Boolean): OutputPostDTO = new OutputPostDTO(
    post.id, post.poster, post.content, post.posted, post.likes, liked)
}
