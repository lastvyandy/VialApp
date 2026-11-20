package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Capítulos y pasos del camino a la licencia. Clase C tiene etapas de moto propias.

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class LearningProgress(
    val answers: Map<String, Int>,
    @SerializedName("completed_lessons") val completedLessons: List<String>,
    @SerializedName("quiz_completed") val quizCompleted: Boolean,
    val score: Int,
    val completed: Boolean
)

data class LearningLessonSummary(
    val id: String, val title: String, val subtitle: String,
    @SerializedName("concept_ids") val conceptIds: List<String>
)

data class LearningChapterSummary(
    val id: String, val title: String, val order: Int,
    @SerializedName("lesson_count") val lessonCount: Int,
    val lessons: List<LearningLessonSummary>,
    @SerializedName("quiz_count") val quizCount: Int,
    @SerializedName("flashcard_count") val flashcardCount: Int,
    val progress: LearningProgress
) {
    val totalSteps get() = lessons.sumOf { it.conceptIds.size } + quizCount
    val answeredSteps get() = progress.answers.size.coerceAtMost(totalSteps)
}

data class LearningPathResponse(val chapters: List<LearningChapterSummary>, val gamification: GamificationProgressResponse)

data class LearningQuestion(
    val id: String? = null,
    @SerializedName("q") val prompt: String,
    @SerializedName("o") val options: List<String>
)

data class LearningConcept(val id: String, val title: String, val blocks: List<JsonElement>, val question: LearningQuestion)

data class LearningLesson(val id: String, val title: String, val subtitle: String, val concepts: List<LearningConcept>)

data class LearningChapter(
    val id: String, val title: String, val order: Int,
    val lessons: List<LearningLesson>, val quiz: List<LearningQuestion>,
    val flashcards: List<List<String>>, val source: List<JsonElement>
)

data class LearningChapterResponse(val chapter: LearningChapter, val progress: LearningProgress, val gamification: GamificationProgressResponse)

data class LearningAnswerRequest(val answer: Int)
data class LearningAnswerResponse(
    val progress: LearningProgress,
    val gamification: GamificationProgressResponse,
    @SerializedName("xp_awarded") val xpAwarded: Int,
    val correct: Boolean,
    @SerializedName("correct_answer") val correctAnswer: Int,
    val explanation: String
)
