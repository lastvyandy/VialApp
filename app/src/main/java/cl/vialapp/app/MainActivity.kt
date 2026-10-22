package cl.vialapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.ui.theme.VialAppTheme
import androidx.compose.runtime.rememberCoroutineScope
import cl.vialapp.app.data.api.ApiClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import cl.vialapp.app.data.api.UserResponse
import androidx.compose.ui.platform.LocalContext
import cl.vialapp.app.data.session.SessionManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import cl.vialapp.app.ui.navigation.VialAppNavigation
import cl.vialapp.app.ui.profile.CompleteProfileScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)

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

                LaunchedEffect(Unit) {

                    val token =
                        sessionManager.getAccessToken()

                    if (token != null) {

                        try {

                            var userResponse =
                                ApiClient.api.getMe(
                                    authorization = "Bearer $token"
                                )


                            // Access token expirado
                            if (userResponse.code() == 401) {

                                val csrfToken =
                                    sessionManager.getCsrfToken()

                                if (csrfToken != null) {

                                    val refreshResponse =
                                        ApiClient.api.refresh(
                                            csrfToken
                                        )

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

                            } else {

                                sessionManager.clearSession()
                                ApiClient.clearCookies()
                            }

                        } catch (e: Exception) {

                            println(e)
                        }
                    }

                    checkingSession = false
                }


                if (checkingSession) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else if (currentUser == null) {

                    LoginScreen(
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
fun LoginScreen(
    onLoginSuccess: (UserResponse) -> Unit
) {

    val context = LocalContext.current
    val sessionManager = remember {
        SessionManager(context)
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var loading by rememberSaveable {
        mutableStateOf(false)
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    val buttonEnabled =
        email.isNotBlank() && password.isNotBlank()

    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF4F8FF),
            Color(0xFFFFFFFF)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 28.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Logo temporal de VialApp
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "VialApp",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Prepárate. Practica. Aprueba.",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(40.dp)
                )

                Text(
                    text = "Bienvenido",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Ingresa a tu cuenta para continuar",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Correo electrónico")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Contraseña")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),

                    visualTransformation =
                        if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },

                    trailingIcon = {
                        TextButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {
                            Text(
                                if (passwordVisible) "Ocultar"
                                else "Mostrar"
                            )
                        }
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                TextButton(
                    onClick = {
                        // Lo programaremos después
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = {

                        scope.launch {

                            loading = true
                            message = ""

                            try {

                                val response = ApiClient.api.login(
                                    email = email,
                                    password = password
                                )

                                if (response.isSuccessful) {

                                    val loginResponse = response.body()

                                    if (loginResponse != null) {

                                        sessionManager.saveAccessToken(
                                            loginResponse.accessToken
                                        )

                                        loginResponse.csrfToken?.let {
                                            sessionManager.saveCsrfToken(it)
                                        }

                                        val userResponse = ApiClient.api.getMe(
                                            authorization = "Bearer ${loginResponse.accessToken}"
                                        )

                                        if (userResponse.isSuccessful) {

                                            val user = userResponse.body()

                                            if (user != null) {
                                                onLoginSuccess(user)
                                            }

                                        } else {

                                            sessionManager.clearSession()
                                            ApiClient.clearCookies()

                                            message = "No se pudo obtener tu perfil"
                                        }
                                    }

                                } else {

                                    message = "Correo o contraseña incorrectos"
                                }

                            } catch (e: Exception) {

                                message = "No se pudo conectar con VialApp"

                                println(e)

                            } finally {

                                loading = false
                            }
                        }
                    },

                    enabled = buttonEnabled && !loading,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    )
                ) {

                    Text(
                        text = if (loading) {
                            "Iniciando sesión..."
                        } else {
                            "Iniciar sesión"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }


// Mensaje debajo del botón
                if (message.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = message,
                        color = if (message.contains("correcto")) {
                            Color(0xFF16A34A)
                        } else {
                            Color(0xFFDC2626)
                        },
                        fontSize = 14.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "¿Aún no tienes una cuenta?",
                        color = Color(0xFF6B7280)
                    )

                    TextButton(
                        onClick = {
                            // Registro después
                        }
                    ) {
                        Text(
                            text = "Crear cuenta",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = "VialApp · Educación vial",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp
            )
        }
    }
}


