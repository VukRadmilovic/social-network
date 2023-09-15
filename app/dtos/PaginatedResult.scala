package dtos

case class PaginatedResult[T](
    totalCount: Int,
    entries: List[T],
    hasNextPage: Boolean
)
