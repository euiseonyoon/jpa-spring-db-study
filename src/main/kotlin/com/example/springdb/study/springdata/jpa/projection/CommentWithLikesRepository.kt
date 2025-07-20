package com.example.springdb.study.springdata.jpa.projection

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentWithLikesRepository : JpaRepository<CommentWithLikes, Long> {

    fun findByPost_Id(id: Long): List<CommentWithLikes>

    @Query("SELECT c FROM CommentWithLikes c WHERE c.post.id = :id")
    fun findWithPost(@Param("id") id: Long): List<CommentProjection>

    @Query(
        "SELECT new com.example.springdb.study.springdata.jpa.projection.CommentDto(c.likes, c.dislikes) FROM CommentWithLikes c WHERE c.post.id = :id"
    )
    fun findCommentDtoById(@Param("id") id: Long): List<CommentDto>
}
