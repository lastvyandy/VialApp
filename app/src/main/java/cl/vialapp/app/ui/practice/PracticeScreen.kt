package cl.vialapp.app.ui.practice

// GUÍA DEL ARCHIVO:
// Entrada a Practicar. Aunque el backend tarde, la pantalla debe seguir siendo útil.

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.CategoryStatResponse
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * La idea es que Practicar le diga al alumno qué hacer, no que sea una lista de botones.
 * La pantalla se muestra igual aunque las estadísticas tarden o falle la conexión.
 */
@Composable
fun PracticeScreen(
    onPracticeCategory:(String)->Unit,
    onTakeExam:()->Unit,
    onLearningPath:()->Unit={},
    onAdaptivePractice:()->Unit={},
    onSimulator:()->Unit={},
    onGames:()->Unit={},
    onLibrary:()->Unit={},
    onOfflineStudy:()->Unit=onLibrary
){
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    var stats by remember{mutableStateOf<List<CategoryStatResponse>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf("")}
    var retry by remember{mutableIntStateOf(0)}

    // No dejamos la pantalla esperando indefinidamente al servidor. Si falla, el alumno puede seguir estudiando.
    LaunchedEffect(retry){
        loading=true
        error=""
        try {
            val token=session.getAccessToken()
            when {
                token==null->error="No se encontró una sesión válida."
                else->{
                    val result=withTimeoutOrNull(8_000L){
                        ApiClient.api.getCategoryStats(authorization="Bearer $token")
                    }
                    when {
                        result==null->error="La consulta de estadísticas tardó demasiado."
                        !result.isSuccessful->error="No pudimos actualizar tus estadísticas (${result.code()})."
                        else->{
                            stats=result.body()?:emptyList()
                        }
                    }
                }
            }
        }catch(cancelled:CancellationException){
            throw cancelled
        }catch(_:Exception){
            error="No se pudo conectar con VialApp."
        }finally{
            loading=false
        }
    }

    PracticeDashboard(
        stats=stats,
        loading=loading,
        error=error,
        onRetry={retry++},
        onPracticeCategory=onPracticeCategory,
        onTakeExam=onTakeExam,
        onLearningPath=onLearningPath,
        onAdaptivePractice=onAdaptivePractice,
        onSimulator=onSimulator,
        onGames=onGames,
        onLibrary=onLibrary,
        onOfflineStudy=onOfflineStudy
    )
}
