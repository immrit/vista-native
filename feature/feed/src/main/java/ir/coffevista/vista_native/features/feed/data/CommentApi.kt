package ir.coffevista.vista_native.features.feed.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommentApi {
    @GET("v1/comments")
    suspend fun getComments(
        @Query("post_id") postId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): CommentListResponseDto

    @POST("v1/comments")
    suspend fun createComment(
        @Body request: CreateCommentRequestDto,
    ): CommentResponseDto

    @DELETE("v1/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String,
    )

    @retrofit2.http.PATCH("v1/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: String,
        @Body request: UpdateCommentRequestDto,
    ): CommentResponseDto

    @POST("v1/comments/{commentId}/report")
    suspend fun reportComment(
        @Path("commentId") commentId: String,
        @Body request: ReportCommentRequestDto,
    )

    @POST("v1/comments/{commentId}/mentions")
    suspend fun addMentions(
        @Path("commentId") commentId: String,
        @Body request: CommentMentionsRequestDto,
    )
}
