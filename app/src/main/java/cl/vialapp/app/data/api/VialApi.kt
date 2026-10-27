package cl.vialapp.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VialApi {

    @FormUrlEncoded
    @POST("users/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("users/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @POST("users/refresh")
    suspend fun refresh(
        @Header("X-CSRF-Token") csrfToken: String
    ): Response<LoginResponse>

    @POST("users/logout")
    suspend fun logout(
        @Header("X-CSRF-Token") csrfToken: String
    ): Response<Unit>

    @PATCH("users/me/complete-profile")
    suspend fun completeProfile(
        @Header("Authorization") authorization: String,
        @Body profile: CompleteProfileRequest
    ): Response<UserResponse>

    @GET("tests/")
    suspend fun getTests(
        @Header("Authorization") authorization: String,
        @Query("clase_licencia") licenseClass: String? = null,
        @Query("categoria") category: String? = null
    ): Response<TestsResponse>

    @POST("tests/{testId}/attempts/start")
    suspend fun startExam(
        @Header("Authorization") authorization: String,
        @Path("testId") testId: Int
    ): Response<StartExamResponse>

    @PATCH("tests/attempts/{attemptId}/answers")
    suspend fun saveExamAnswers(
        @Header("Authorization") authorization: String,
        @Path("attemptId") attemptId: Int,
        @Body request: SaveAnswersRequest
    ): Response<ExamAttemptResponse>

    @GET("tests/active-attempt")
    suspend fun getActiveAttempt(
        @Header("Authorization") authorization: String
    ): Response<ExamAttemptResponse?>

    @POST("tests/{testId}/submit")
    suspend fun submitExam(
        @Header("Authorization") authorization: String,
        @Path("testId") testId: Int,
        @Body request: SubmitExamRequest
    ): Response<ExamSubmitResponse>

    @GET("users/me/tests/{resultId}")
    suspend fun getExamCorrection(
        @Header("Authorization") authorization: String,
        @Path("resultId") resultId: Int
    ): Response<ExamCorrectionResponse>

    @GET("users/me/stats/categorias")
    suspend fun getCategoryStats(
        @Header("Authorization") authorization: String
    ): Response<List<CategoryStatResponse>>

    @GET("users/me/stats/avanzadas")
    suspend fun getAdvancedStats(
        @Header("Authorization") authorization: String
    ): Response<AdvancedStatsResponse>

    @GET("tests/{testId}/study")
    suspend fun getStudyTest(
        @Header("Authorization") authorization: String,
        @Path("testId") testId: Int
    ): Response<StudyTestResponse>

    @GET("users/me/gamification")
    suspend fun getGamificationProgress(
        @Header("Authorization") authorization: String
    ): Response<GamificationProgressResponse>

    @POST("users/me/gamification/games")
    suspend fun recordGameEvent(
        @Header("Authorization") authorization: String,
        @Body request: GamificationGameEventRequest
    ): Response<GamificationEventResponse>
}