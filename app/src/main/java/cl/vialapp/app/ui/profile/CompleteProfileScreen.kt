package cl.vialapp.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.CompleteProfileRequest
import cl.vialapp.app.data.api.UserResponse
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.launch


private val VialBlue = Color(0xFF2563EB)
private val VialDark = Color(0xFF172033)
private val VialGray = Color(0xFF64748B)


@Composable
fun CompleteProfileScreen(
    onProfileCompleted: (UserResponse) -> Unit
) {

    val context = LocalContext.current

    val sessionManager = remember {
        SessionManager(context)
    }

    val scope = rememberCoroutineScope()

    var firstName by remember {
        mutableStateOf("")
    }

    var lastName by remember {
        mutableStateOf("")
    }

    var selectedLicense by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val canContinue =
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                (selectedLicense == "B" || selectedLicense == "C") &&
                !loading


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            text = "Completa tu perfil",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = VialDark
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Cuéntanos un poco de ti para preparar VialApp según tu licencia.",
            fontSize = 16.sp,
            color = VialGray
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )


        Text(
            text = "Tu nombre",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = VialDark
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = firstName,
            onValueChange = {
                firstName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Nombre")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = {
                lastName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Apellidos")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )


        Spacer(
            modifier = Modifier.height(32.dp)
        )


        Text(
            text = "¿Qué licencia estás preparando?",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = VialDark
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Podrás cambiarla más adelante desde tu perfil.",
            color = VialGray,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )


        LicenseOption(
            emoji = "🚗",
            title = "Clase B",
            subtitle = "Automóviles y vehículos particulares",
            selected = selectedLicense == "B",
            onClick = {
                selectedLicense = "B"
            }
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        LicenseOption(
            emoji = "🏍️",
            title = "Clase C",
            subtitle = "Motocicletas y vehículos similares",
            selected = selectedLicense == "C",
            onClick = {
                selectedLicense = "C"
            }
        )


        if (errorMessage.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = errorMessage,
                color = Color(0xFFDC2626),
                fontSize = 14.sp
            )
        }


        Spacer(
            modifier = Modifier.height(32.dp)
        )


        Button(
            enabled = canContinue,
            onClick = {

                scope.launch {

                    loading = true
                    errorMessage = ""

                    try {

                        val token =
                            sessionManager.getAccessToken()

                        if (token == null) {

                            errorMessage =
                                "No se encontró una sesión válida"

                            loading = false

                            return@launch
                        }

                        val response =
                            ApiClient.api.completeProfile(
                                authorization =
                                    "Bearer $token",

                                profile =
                                    CompleteProfileRequest(
                                        firstName =
                                            firstName.trim(),

                                        lastName =
                                            lastName.trim(),

                                        licenseClass =
                                            selectedLicense!!
                                    )
                            )


                        if (response.isSuccessful) {

                            val user =
                                response.body()

                            if (user != null) {

                                onProfileCompleted(
                                    user
                                )

                            } else {

                                errorMessage =
                                    "No se pudo actualizar tu perfil"
                            }

                        } else {

                            errorMessage =
                                "No se pudo completar tu perfil"
                        }

                    } catch (e: Exception) {

                        errorMessage =
                            "Error de conexión con VialApp"

                        println(e)

                    } finally {

                        loading = false
                    }
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        VialBlue
                )
        ) {

            Text(
                text =
                    if (loading) {
                        "Guardando..."
                    } else {
                        "Continuar"
                    },

                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}


@Composable
private fun LicenseOption(
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val backgroundColor =
        if (selected) {
            Color(0xFFE8F0FF)
        } else {
            Color.White
        }

    val borderColor =
        if (selected) {
            VialBlue
        } else {
            Color(0xFFE2E8F0)
        }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),

        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = emoji,
                fontSize = 34.sp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp)
            ) {

                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color = VialDark
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = VialGray
                )
            }


            RadioButton(
                selected = selected,
                onClick = onClick,

                colors =
                    RadioButtonDefaults.colors(
                        selectedColor =
                            VialBlue
                    )
            )
        }
    }
}
