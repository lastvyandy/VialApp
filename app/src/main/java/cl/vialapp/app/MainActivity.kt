package cl.vialapp.app

import android.os.Bundle
import android.content.Intent
import cl.vialapp.app.engagement.EngagementState
import cl.vialapp.app.engagement.StudyReminder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.ui.theme.VialAppTheme
import cl.vialapp.app.ui.theme.vialColor
import cl.vialapp.app.ui.theme.VialActionBlue
import androidx.compose.runtime.rememberCoroutineScope
import cl.vialapp.app.data.api.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import cl.vialapp.app.ui.library.OfflineStudyScreen
import androidx.compose.runtime.remember
import cl.vialapp.app.data.api.UserResponse
import androidx.compose.ui.platform.LocalContext
import cl.vialapp.app.data.session.SessionManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import cl.vialapp.app.ui.navigation.VialAppNavigation
import cl.vialapp.app.ui.profile.CompleteProfileScreen

class MainActivity : ComponentActivity() {
    private var openPractice by mutableStateOf(false)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_practice",openPractice)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == StudyReminder.OPEN_PRACTICE) {
            openPractice = true
            intent.action = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)
        runCatching { StudyReminder.channel(applicationContext) }
        openPractice = savedInstanceState?.getBoolean("pending_practice",false) == true || intent.action == StudyReminder.OPEN_PRACTICE
        if(intent.action == StudyReminder.OPEN_PRACTICE) intent.action = null

        enableEdgeToEdge()

        setContent {

            VialAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val sessionManager = remember {
                    SessionManager(context)
                }

                var currentUser by remember {
                    mutableStateOf<UserResponse?>(null)
                }

                var checkingSession by remember {
                    mutableStateOf(true)
                }

                var retrySession by remember { mutableStateOf(0) }
                var offlineStudy by rememberSaveable { mutableStateOf(false) }
                var sessionUnavailable by remember { mutableStateOf(false) }

                LaunchedEffect(retrySession) {
                    checkingSession = true
                    sessionUnavailable = false

                    val token =
                        sessionManager.getAccessToken()

                    if (token != null) {

                        try {

                            var userResponse =
                                ApiClient.api.getMe(
                                    authorization = "Bearer $token"
                                )


                            var refreshRejected = false
                            // Access token expirado
                            if (userResponse.code() == 401) {

                                val csrfToken =
                                    sessionManager.getCsrfToken()

                                if (csrfToken != null) {

                                    val refreshResponse =
                                        ApiClient.api.refresh(
                                            csrfToken
                                        )

                                    refreshRejected = refreshResponse.code() in listOf(401,403)
                                    if (!refreshResponse.isSuccessful && !refreshRejected) {
                                        sessionUnavailable = true
                                        checkingSession = false
                                        return@LaunchedEffect
                                    }
                                    if (refreshResponse.isSuccessful) {

                                        val newTokens =
                                            refreshResponse.body()

                                        if (newTokens != null) {

                                            sessionManager.saveAccessToken(
                                                newTokens.accessToken
                                            )

                                            newTokens.csrfToken?.let {
                                                sessionManager.saveCsrfToken(
                                                    it
                                                )
                                            }

                                            // Intentamos nuevamente /me
                                            userResponse =
                                                ApiClient.api.getMe(
                                                    authorization =
                                                        "Bearer ${newTokens.accessToken}"
                                                )
                                        }
                                    }
                                }
                            }


                            if (userResponse.isSuccessful) {

                                currentUser =
                                    userResponse.body()

                            } else if (userResponse.code() in listOf(401,403) || refreshRejected) {
                                sessionManager.clearSession()
                                ApiClient.clearCookies()
                            } else {
                                // Temporary server failures must not erase valid credentials.
                                sessionUnavailable = true
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            sessionUnavailable = true
                        }
                    }

                    checkingSession = false
                }


                LaunchedEffect(currentUser?.id,currentUser?.mascotId,checkingSession) {
                    if(!checkingSession) {
                        val verified=currentUser
                        if(verified!=null) EngagementState.activate(applicationContext,verified.id,verified.mascotId)
                        else if(sessionManager.getAccessToken().isNullOrBlank()) EngagementState.deactivate(applicationContext)
                    }
                }

                if (offlineStudy) {
                    OfflineStudyScreen(onBack={offlineStudy=false})
                } else if (checkingSession) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text("Conectando con VialApp")
                            TextButton(onClick={offlineStudy=true}){Text("Estudiar sin conexión")}
                        }
                    }
                } else if(sessionUnavailable && currentUser==null) {
                    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("No pudimos conectar",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("Tu sesión se conserva. Puedes estudiar sin internet o volver a intentar.",textAlign=TextAlign.Center)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick={retrySession++},modifier=Modifier.fillMaxWidth()){Text("Reintentar conexión")}
                        TextButton(onClick={offlineStudy=true}){Text("Estudiar sin conexión")}
                        TextButton(onClick={sessionManager.clearSession();ApiClient.clearCookies();sessionUnavailable=false}){Text("Cerrar sesión")}
                    }
                } else if (currentUser == null) {

                    LoginScreen(
                        onOfflineStudy = { offlineStudy=true },
                        onLoginSuccess = { user ->
                            currentUser = user
                        }
                    )

                }

                else if (currentUser?.profileCompleted == false) {

                    CompleteProfileScreen(
                        onProfileCompleted = { user ->

                            currentUser = user
                        }
                    )

                }

                else {

                    VialAppNavigation(
                        user = currentUser!!,
                        openPracticeRequested = openPractice,
                        onPracticeRequestHandled = { openPractice = false },
                        onUserUpdated = { updatedUser -> currentUser = updatedUser },
                        onLogout = {

                            scope.launch {

                                val csrfToken =
                                    sessionManager.getCsrfToken()

                                try {

                                    if (csrfToken != null) {

                                        ApiClient.api.logout(
                                            csrfToken
                                        )
                                    }

                                } catch (e: Exception) {

                                    println(e)

                                } finally {

                                    sessionManager.clearSession()
                                    ApiClient.clearCookies()

                                    currentUser = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun LoginScreen(onLoginSuccess:(UserResponse)->Unit,onOfflineStudy:()->Unit={}){
    cl.vialapp.app.ui.auth.AuthScreen(onLoginSuccess,onOfflineStudy)
}
