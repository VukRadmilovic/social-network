package models

import dtos.InputPostDTO
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class Post (
                  id: Long,
                  poster: String,
                  content: String,
                  posted: LocalDateTime,
                  likes: Long
)

object Post {
  implicit val jsonFormat: OFormat[Post] = Json.format[Post]

  def create(postDTO: InputPostDTO, poster: String): Post = new Post(0, poster, postDTO.content, LocalDateTime.now, 0)
  def create(post: Post, id: Long): Post = new Post(id, post.poster, post.content, post.posted, post.likes)
}
