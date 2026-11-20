package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Acá van los endpoints. Si cambias una ruta, revisa el backend; no hacemos magia por Wi-Fi, jajaj.

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.DELETE
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
    @GET("config/features")
    suspend fun getPublicFeatures(): Response<PublicFeatures>

    @POST("users/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

    @POST("users/me/google-link")
    suspend fun linkGoogle(@Header("Authorization") authorization:String,@Body request:GoogleLinkRequest):Response<UserResponse>

    @POST("payments/google-play/verify")
    suspend fun verifyPlayPurchase(@Header("Authorization") authorization:String,@Body request:PlayPurchaseVerify):Response<PlayPurchaseResult>


    @POST("users/")
    suspend fun register(@Body request: RegistrationRequest): Response<UserResponse>

    @POST("users/password-reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<PasswordResetMessage>

    @FormUrlEncoded
    @POST("users/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("users/me/avatar")
    suspend fun getMyAvatar(@Header("Authorization") authorization: String): Response<AvatarResponse>

    @PUT("users/me/avatar")
    suspend fun putMyAvatar(
        @Header("Authorization") authorization: String,
        @Body photo: RequestBody
    ): Response<AvatarResponse>

    @DELETE("users/me/avatar")
    suspend fun deleteMyAvatar(@Header("Authorization") authorization: String): Response<AvatarResponse>

    @GET("users/avatars/{key}")
    suspend fun getAvatar(
        @Header("Authorization") authorization: String,
        @Path("key") key: String
    ): Response<ResponseBody>

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

    @PATCH("users/me")
    suspend fun updateMascot(
        @Header("Authorization") authorization: String,
        @Body request: MascotSelectionRequest
    ): Response<UserResponse>

    @PATCH("users/me")
    suspend fun updateLicenseClass(
        @Header("Authorization") authorization: String,
        @Body request: LicenseClassUpdateRequest
    ): Response<UserResponse>

    @GET("tests/")
    suspend fun getTests(
        @Header("Authorization") authorization: String,
        @Query("clase_licencia") licenseClass: String? = null,
        @Query("categoria") category: String? = null,
        @Query("history_limit") historyLimit: Int = 50,
        @Query("history_offset") historyOffset: Int = 0
    ): Response<TestsResponse>

    @GET("tests/datos_tests")
    suspend fun getExamCatalogCounts(): Response<List<ExamCategoryCount>>

    @GET("tests/catalogo")
    suspend fun getExamCatalogPage(
        @Header("Authorization") authorization: String,
        @Query("clase_licencia") licenseClass: String?,
        @Query("categoria") category: String?,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<ExamCatalogPage>

    @GET("users/me/tests")
    suspend fun getMyExamHistory(
        @Header("Authorization") authorization: String
    ): Response<List<ExamHistoryResult>>

    @POST("tests/unlock-extra")
    suspend fun unlockExtraExam(
        @Header("Authorization") authorization: String,
        @Body request: ExtraExamRequest
    ): Response<AssignedTest>

    @POST("tests/{testId}/attempts/start")
    suspend fun startExam(
        @Header("Authorization") authorization: String,
        @Path("testId") testId: Int
    ): Response<StartExamResponse>

    @POST("tests/attempts/{attemptId}/cancel")
    suspend fun cancelExamAttempt(
        @Header("Authorization") authorization: String,
        @Path("attemptId") attemptId: Int
    ): Response<ExamAttemptResponse>

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

    @POST("questions/adaptive/start")
    suspend fun startAdaptivePractice(
        @Header("Authorization") authorization: String,
        @Query("clase_licencia") licenseClass: String?
    ): Response<AdaptivePracticeSession>

    @POST("questions/adaptive/submit")
    suspend fun submitAdaptivePractice(
        @Header("Authorization") authorization: String,
        @Body request: AdaptivePracticeSubmission
    ): Response<AdaptivePracticeResult>

    @GET("users/me/stats/categorias")
    suspend fun getCategoryStats(
        @Header("Authorization") authorization: String
    ): Response<List<CategoryStatResponse>>

    @GET("users/me/stats/readiness")
    suspend fun getExamReadiness(
        @Header("Authorization") authorization: String,
        @Query("clase_licencia") licenseClass: String? = null
    ): Response<ExamReadinessResponse>

    @GET("users/me/stats/avanzadas")
    suspend fun getAdvancedStats(
        @Header("Authorization") authorization: String
    ): Response<AdvancedStatsResponse>

    @GET("tests/{testId}/study")
    suspend fun getStudyTest(
        @Header("Authorization") authorization: String,
        @Path("testId") testId: Int
    ): Response<StudyTestResponse>

    @GET("learning/simulator")
    suspend fun getSimulator(@Header("Authorization") authorization: String,
        @Query("scene_version") sceneVersion: Int = 2): Response<SimulatorCatalog>

    @POST("learning/simulator/{scenarioId}/complete")
    suspend fun completeSimulator(@Header("Authorization") authorization: String,
        @Path("scenarioId") scenarioId: String,
        @Body request: SimulatorCompletionRequest): Response<SimulatorCompletionResponse>

    @GET("learning")
    suspend fun getLearningPath(@Header("Authorization") authorization: String): Response<LearningPathResponse>

    @GET("learning/{chapterId}")
    suspend fun getLearningChapter(@Header("Authorization") authorization: String,
        @Path("chapterId") chapterId: String): Response<LearningChapterResponse>

    @POST("learning/{chapterId}/steps/{stepId}")
    suspend fun answerLearningStep(@Header("Authorization") authorization: String,
        @Path("chapterId") chapterId: String, @Path("stepId") stepId: String,
        @Body request: LearningAnswerRequest): Response<LearningAnswerResponse>

    @GET("users/me/gamification/leaderboard/weekly")
    suspend fun getWeeklyLeaderboard(
        @Header("Authorization") authorization: String,
        @Query("week_offset") weekOffset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<WeeklyLeaderboardResponse>

    @POST("users/me/gamification/achievements")
    suspend fun syncAchievements(@Header("Authorization") authorization: String): Response<AchievementCollectionResponse>

    @GET("users/me/gamification/career")
    suspend fun getCareer(@Header("Authorization") authorization: String): Response<CareerState>

    @POST("users/me/gamification/career")
    suspend fun changeCareer(@Header("Authorization") authorization: String,
        @Body action: CareerAction): Response<CareerState>

    @GET("users/me/gamification")
    suspend fun getGamificationProgress(
        @Header("Authorization") authorization: String
    ): Response<GamificationProgressResponse>


    @POST("users/me/gamification/games")
    suspend fun recordGameEvent(
        @Header("Authorization") authorization: String,
        @Body request: GamificationGameEventRequest
    ): Response<GamificationEventResponse>

    // Consultar la misión diaria del usuario.
    @GET("users/me/gamification/missions/today")
    suspend fun getTodayMission(
        @Header("Authorization") authorization: String
    ): Response<DailyMissionResponse>

    // Reclamar la recompensa de una misión completada.
    @POST("users/me/gamification/missions/today/claim")
    suspend fun claimTodayMission(
        @Header("Authorization") authorization: String
    ): Response<GamificationEventResponse>
}
