package cl.vialapp.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.UserResponse


private val ProfileBlue = Color(0xFF2563EB)
private val ProfileDark = Color(0xFF172033)
private val ProfileGray = Color(0xFF64748B)
private val ProfileGreen = Color(0xFF14966F)
private val ProfileOrange = Color(0xFFF59E0B)
private val ProfileBackground = Color(0xFFF4F7FB)
private val ProfileRed = Color(0xFFD92D20)


@Composable
fun ProfileScreen(
    user: UserResponse,
    onLogout: () -> Unit
) {

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }


    val fullName =
        listOfNotNull(
            user.firstName
                ?.takeIf {
                    it.isNotBlank()
                },

            user.lastName
                ?.takeIf {
                    it.isNotBlank()
                }
        )
            .joinToString(
                " "
            )
            .ifBlank {
                user.username
            }


    val initials =
        buildString {

            user.firstName
                ?.firstOrNull()
                ?.let {
                    append(
                        it.uppercaseChar()
                    )
                }

            user.lastName
                ?.firstOrNull()
                ?.let {
                    append(
                        it.uppercaseChar()
                    )
                }
        }
            .ifBlank {
                user.email
                    .firstOrNull()
                    ?.uppercaseChar()
                    ?.toString()
                    ?: "V"
            }


    val licenseText =
        user.licenseClass
            ?.let { license ->

                if (
                    license.startsWith(
                        "Clase",
                        ignoreCase = true
                    )
                ) {

                    license

                } else {

                    "Clase $license"
                }
            }
            ?: "No seleccionada"


    if (showLogoutDialog) {

        AlertDialog(

            onDismissRequest = {
                showLogoutDialog = false
            },

            title = {

                Text(
                    text =
                        "¿Cerrar sesión?"
                )
            },

            text = {

                Text(
                    text =
                        "Tendrás que iniciar sesión nuevamente para volver a entrar a VialApp."
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        showLogoutDialog = false
                        onLogout()
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ProfileRed
                        )
                ) {

                    Text(
                        text =
                            "Cerrar sesión"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {

                    Text(
                        text =
                            "Cancelar"
                    )
                }
            }
        )
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ProfileBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                )
                .padding(
                    top = 24.dp,
                    bottom = 110.dp
                )
    ) {


        /*
         * TÍTULO
         */
        Text(
            text =
                "Tu perfil",

            color =
                ProfileDark,

            fontSize =
                30.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(
            text =
                "Tu cuenta y configuración de VialApp.",

            color =
                ProfileGray,

            fontSize =
                15.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )


        /*
         * CABECERA DEL USUARIO
         */
        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    26.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            22.dp
                        ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                /*
                 * AVATAR
                 */
                Surface(
                    modifier =
                        Modifier.size(
                            82.dp
                        ),

                    shape =
                        CircleShape,

                    color =
                        Color(
                            0xFFEAF1FF
                        )
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                initials,

                            color =
                                ProfileBlue,

                            fontSize =
                                30.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                Text(
                    text =
                        fullName,

                    color =
                        ProfileDark,

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.Bold,

                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(
                    text =
                        user.email,

                    color =
                        ProfileGray,

                    fontSize =
                        14.sp,

                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                /*
                 * ESTADO DE CUENTA
                 */
                Surface(
                    shape =
                        RoundedCornerShape(
                            30.dp
                        ),

                    color =
                        if (user.isPremium) {

                            Color(
                                0xFFFFF4D8
                            )

                        } else {

                            Color(
                                0xFFF1F5F9
                            )
                        }
                ) {

                    Text(
                        text =
                            if (user.isPremium) {

                                "⭐ Premium"

                            } else {

                                "Cuenta gratuita"
                            },

                        modifier =
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 7.dp
                            ),

                        color =
                            if (user.isPremium) {

                                Color(
                                    0xFF9A6700
                                )

                            } else {

                                ProfileGray
                            },

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        /*
         * LICENCIA
         */
        Text(
            text =
                "Tu licencia",

            color =
                ProfileDark,

            fontSize =
                19.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            18.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Surface(
                    modifier =
                        Modifier.size(
                            50.dp
                        ),

                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),

                    color =
                        Color(
                            0xFFEAF1FF
                        )
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "🚘",

                            fontSize =
                                25.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            15.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            "Licencia seleccionada",

                        color =
                            ProfileGray,

                        fontSize =
                            12.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            licenseText,

                        color =
                            ProfileDark,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        /*
         * PLAN
         */
        Text(
            text =
                "Tu plan",

            color =
                ProfileDark,

            fontSize =
                19.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    22.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (user.isPremium) {

                            Color(
                                0xFFFFF8E8
                            )

                        } else {

                            Color.White
                        }
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        20.dp
                    )
            ) {


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    Text(
                        text =
                            if (user.isPremium) {
                                "⭐"
                            } else {
                                "🌱"
                            },

                        fontSize =
                            28.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                12.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            text =
                                if (user.isPremium) {

                                    "VialApp Premium"

                                } else {

                                    "Plan gratuito"
                                },

                            color =
                                ProfileDark,

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                if (user.isPremium) {

                                    "Tienes acceso a las funciones Premium disponibles para tu cuenta."

                                } else {

                                    "Estás usando las funciones gratuitas de VialApp."
                                },

                            color =
                                ProfileGray,

                            fontSize =
                                13.sp,

                            lineHeight =
                                19.sp,

                            modifier =
                                Modifier.padding(
                                    top = 4.dp
                                )
                        )
                    }
                }


                if (!user.isPremium) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                15.dp
                            )
                    )


                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            Color(
                                0xFFF8FAFD
                            ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {

                        Text(
                            text =
                                "Premium permite ampliar el acceso a exámenes, correcciones y modos de práctica.",

                            modifier =
                                Modifier.padding(
                                    13.dp
                                ),

                            color =
                                ProfileGray,

                            fontSize =
                                12.sp,

                            lineHeight =
                                18.sp
                        )
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )


        /*
         * INFORMACIÓN DE CUENTA
         */
        Text(
            text =
                "Cuenta",

            color =
                ProfileDark,

            fontSize =
                19.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Column {

                ProfileInfoRow(
                    icon =
                        "👤",

                    title =
                        "Nombre",

                    value =
                        fullName
                )


                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp
                        ),

                    color =
                        Color(
                            0xFFE8EDF5
                        )
                )


                ProfileInfoRow(
                    icon =
                        "✉️",

                    title =
                        "Correo electrónico",

                    value =
                        user.email
                )


                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp
                        ),

                    color =
                        Color(
                            0xFFE8EDF5
                        )
                )


                ProfileInfoRow(
                    icon =
                        "🪪",

                    title =
                        "Clase de licencia",

                    value =
                        licenseText
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )


        /*
         * CERRAR SESIÓN
         */
        OutlinedButton(
            onClick = {
                showLogoutDialog = true
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        54.dp
                    ),

            shape =
                RoundedCornerShape(
                    16.dp
                ),

            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor =
                        ProfileRed
                )
        ) {

            Text(
                text =
                    "Cerrar sesión",

                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Text(
            text =
                "VialApp · Educación vial",

            modifier =
                Modifier.fillMaxWidth(),

            color =
                Color(
                    0xFF94A3B8
                ),

            fontSize =
                12.sp,

            textAlign =
                TextAlign.Center
        )
    }
}


@Composable
private fun ProfileInfoRow(
    icon: String,
    title: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    18.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        Surface(
            modifier =
                Modifier.size(
                    42.dp
                ),

            shape =
                RoundedCornerShape(
                    13.dp
                ),

            color =
                Color(
                    0xFFF4F7FB
                )
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        icon,

                    fontSize =
                        20.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.width(
                    13.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,

                color =
                    ProfileGray,

                fontSize =
                    11.sp
            )


            Text(
                text =
                    value,

                color =
                    ProfileDark,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.SemiBold,

                modifier =
                    Modifier.padding(
                        top = 2.dp
                    )
            )
        }
    }
}