package cl.vialapp.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.vialapp.app.R
import cl.vialapp.app.data.api.UserResponse
import cl.vialapp.app.data.settings.StudyPreferencesStore
import cl.vialapp.app.ui.exam.ExamScreen
import cl.vialapp.app.ui.exams.ExamsScreen
import cl.vialapp.app.ui.games.GamesScreen
import cl.vialapp.app.ui.games.baldchallenge.BaldChallengeScreen
import cl.vialapp.app.ui.games.crossroads.CrossroadsChaosScreen
import cl.vialapp.app.ui.games.signhunt.SignHuntScreen
import cl.vialapp.app.ui.home.HomeScreen
import cl.vialapp.app.ui.practice.PracticeScreen
import cl.vialapp.app.ui.progress.ProgressScreen
import cl.vialapp.app.ui.settings.StudySettingsScreen
import cl.vialapp.app.ui.settings.StudySetupScreen

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: Int
)


@Composable
fun VialAppNavigation(
    user: UserResponse,
    onLogout: () -> Unit
) {

    val context =
        LocalContext.current

    val studyPreferencesStore =
        remember(
            user.id
        ) {
            StudyPreferencesStore(
                context =
                    context,
                userKey =
                    user.id
            )
        }

    /*
     * La configuración inicial se muestra una sola vez
     * por cuenta en este dispositivo.
     */
    val initialDestination =
        remember(
            user.id
        ) {
            if (
                studyPreferencesStore
                    .isConfigured()
            ) {
                "home"
            } else {
                "study_setup"
            }
        }


    val navController =
        rememberNavController()


    val backStackEntry by
    navController
        .currentBackStackEntryAsState()


    val currentRoute =
        backStackEntry
            ?.destination
            ?.route


    /*
     * BARRA INFERIOR
     */
    val items =
        listOf(

            BottomItem(
                route =
                    "home",

                label =
                    "Inicio",

                icon =
                    R.drawable.ic_home
            ),

            BottomItem(
                route =
                    "practice",

                label =
                    "Practicar",

                icon =
                    R.drawable.ic_practice
            ),

            BottomItem(
                route =
                    "stats",

                label =
                    "Progreso",

                icon =
                    R.drawable.ic_stats
            ),

            BottomItem(
                route =
                    "profile",

                label =
                    "Perfil",

                icon =
                    R.drawable.ic_profile
            )
        )


    /*
     * Navegación para las cuatro
     * secciones principales.
     */
    fun navigateTopLevel(
        route: String
    ) {

        navController.navigate(
            route
        ) {

            popUpTo(
                navController.graph
                    .findStartDestination()
                    .id
            ) {

                saveState =
                    true
            }


            launchSingleTop =
                true


            restoreState =
                true
        }
    }


    /*
     * Ocultamos la barra inferior
     * mientras el usuario responde
     * un examen.
     */
    val showBottomBar =
        currentRoute != "study_setup" &&
                currentRoute != "study_settings" &&
                currentRoute != "exam/{testId}" &&
                currentRoute != "games/bald/{testId}" &&
                currentRoute != "games/signs" &&
                currentRoute != "games/crossroads"


    Scaffold(

        containerColor =
            Color(
                0xFFF7F9FC
            ),

        bottomBar = {

            if (
                showBottomBar
            ) {

                NavigationBar(
                    containerColor =
                        Color.White
                ) {

                    items.forEach { item ->


                        NavigationBarItem(

                            selected =
                                currentRoute ==
                                        item.route,

                            onClick = {

                                navigateTopLevel(
                                    item.route
                                )
                            },

                            icon = {

                                Icon(
                                    painter =
                                        painterResource(
                                            item.icon
                                        ),

                                    contentDescription =
                                        item.label
                                )
                            },

                            label = {

                                Text(
                                    text =
                                        item.label
                                )
                            },

                            colors =
                                NavigationBarItemDefaults
                                    .colors(

                                        selectedIconColor =
                                            Color(
                                                0xFF2563EB
                                            ),

                                        selectedTextColor =
                                            Color(
                                                0xFF2563EB
                                            ),

                                        indicatorColor =
                                            Color(
                                                0xFFE8F0FF
                                            ),

                                        unselectedIconColor =
                                            Color(
                                                0xFF94A3B8
                                            ),

                                        unselectedTextColor =
                                            Color(
                                                0xFF64748B
                                            )
                                    )
                        )
                    }
                }
            }
        }

    ) { innerPadding ->


        NavHost(

            navController =
                navController,

            startDestination =
                initialDestination,

            modifier =
                Modifier.padding(
                    innerPadding
                )

        ) {


            /*
             * ------------------------------------------------
             * CONFIGURACIÓN INICIAL DE ESTUDIO
             * ------------------------------------------------
             *
             * Solo aparece la primera vez para esta cuenta.
             */
            composable(
                route =
                    "study_setup"
            ) {

                StudySetupScreen(
                    userKey =
                        user.id,

                    onFinished = {

                        navController.navigate(
                            "home"
                        ) {

                            popUpTo(
                                "study_setup"
                            ) {

                                inclusive =
                                    true
                            }

                            launchSingleTop =
                                true
                        }
                    }
                )
            }


            /*
             * ------------------------------------------------
             * HOME
             * ------------------------------------------------
             */
            composable(
                route =
                    "home"
            ) {

                HomeScreen(

                    user =
                        user,

                    onPractice = {

                        navigateTopLevel(
                            "practice"
                        )
                    },

                    onExam = {

                        navController.navigate(
                            "exams"
                        )
                    },

                    onProgress = {

                        navigateTopLevel(
                            "stats"
                        )
                    },

                    onGames = {

                        navController.navigate(
                            "games"
                        )
                    },

                    onResumeExam = { testId ->

                        navController.navigate(
                            "exam/$testId"
                        )
                    }
                )
            }




            /*
             * ------------------------------------------------
             * MINIJUEGOS
             * ------------------------------------------------
             */
            composable(
                route =
                    "games"
            ) {

                GamesScreen(
                    licenseClass =
                        user.licenseClass,

                    onOpenBaldChallenge = { testId ->

                        navController.navigate(
                            "games/bald/$testId"
                        )
                    },

                    onOpenCrossroadsChaos = {

                        navController.navigate(
                            "games/crossroads"
                        )
                    },

                    onBack = {

                        navController.popBackStack()
                    }
                )
            }


            /*
             * RETO DEL PELAO
             */
            composable(
                route =
                    "games/bald/{testId}"
            ) { backStackEntry ->

                val testId =
                    backStackEntry
                        .arguments
                        ?.getString(
                            "testId"
                        )
                        ?.toIntOrNull()

                if (
                    testId != null
                ) {

                    BaldChallengeScreen(
                        testId =
                            testId,

                        onBack = {

                            navController.popBackStack()
                        }
                    )

                } else {

                    PlaceholderScreen(
                        title =
                            "Error",

                        message =
                            "No se pudo iniciar el minijuego."
                    )
                }
            }


            /*
             * CRUCE CAÓTICO
             */
            composable(
                route =
                    "games/crossroads"
            ) {

                CrossroadsChaosScreen(
                    onBack = {

                        navController.popBackStack()
                    }
                )
            }


            /*
             * CAZA LA SEÑAL
             *
             * La ruta queda disponible aunque
             * el juego esté en pausa en el hub.
             */
            composable(
                route =
                    "games/signs"
            ) {

                SignHuntScreen(
                    licenseClass =
                        user.licenseClass,

                    onBack = {

                        navController.popBackStack()
                    }
                )
            }


            /*
             * ------------------------------------------------
             * PRACTICAR
             * ------------------------------------------------
             *
             * Aquí ya no usamos
             * PlaceholderScreen.
             */
            composable(
                route =
                    "practice"
            ) {

                PracticeScreen(

                    /*
                     * El usuario toca una
                     * categoría específica.
                     */
                    onPracticeCategory = { category ->

                        val encodedCategory =
                            Uri.encode(
                                category
                            )


                        navController.navigate(
                            "exams/category/$encodedCategory"
                        )
                    },

                    /*
                     * Examen general.
                     */
                    onTakeExam = {

                        navController.navigate(
                            "exams"
                        )
                    }
                )
            }


            /*
             * ------------------------------------------------
             * EXÁMENES NORMALES
             * ------------------------------------------------
             */
            composable(
                route =
                    "exams"
            ) {

                ExamsScreen(

                    licenseClass =
                        user.licenseClass,

                    /*
                     * Sin filtro.
                     */
                    category =
                        null,

                    onExamClick = { testId ->

                        navController.navigate(
                            "exam/$testId"
                        )
                    },

                    onBack = {

                        navController
                            .popBackStack()
                    }
                )
            }


            /*
             * ------------------------------------------------
             * EXÁMENES FILTRADOS POR CATEGORÍA
             * ------------------------------------------------
             */
            composable(

                route =
                    "exams/category/{category}",

                arguments =
                    listOf(

                        navArgument(
                            "category"
                        ) {

                            type =
                                NavType.StringType
                        }
                    )

            ) { backStackEntry ->


                val encodedCategory =
                    backStackEntry
                        .arguments
                        ?.getString(
                            "category"
                        )
                        .orEmpty()


                val category =
                    Uri.decode(
                        encodedCategory
                    )


                ExamsScreen(

                    licenseClass =
                        user.licenseClass,

                    category =
                        category,

                    onExamClick = { testId ->

                        navController.navigate(
                            "exam/$testId"
                        )
                    },

                    onBack = {

                        navController
                            .popBackStack()
                    }
                )
            }


            /*
             * ------------------------------------------------
             * EXAMEN INDIVIDUAL
             * ------------------------------------------------
             */
            composable(
                route =
                    "exam/{testId}"
            ) { backStackEntry ->


                val testId =
                    backStackEntry
                        .arguments
                        ?.getString(
                            "testId"
                        )
                        ?.toIntOrNull()


                if (
                    testId != null
                ) {

                    ExamScreen(

                        testId =
                            testId,

                        onBack = {

                            navController
                                .popBackStack()
                        }
                    )


                } else {

                    PlaceholderScreen(

                        title =
                            "Error",

                        message =
                            "No se pudo abrir este examen."
                    )
                }
            }


            /*
             * ------------------------------------------------
             * PROGRESO
             * ------------------------------------------------
             *
             * Todavía lo dejamos como placeholder.
             */
            composable(
                route = "stats"
            ) {

                ProgressScreen()
            }


            /*
             * ------------------------------------------------
             * CONFIGURACIÓN DE ESTUDIO
             * ------------------------------------------------
             */
            composable(
                route =
                    "study_settings"
            ) {

                StudySettingsScreen(
                    userKey =
                        user.id,

                    onBack = {

                        navController
                            .popBackStack()
                    }
                )
            }


            /*
             * ------------------------------------------------
             * PERFIL
             * ------------------------------------------------
             */
            composable(
                route =
                    "profile"
            ) {

                ProfileScreen(
                    user =
                        user,

                    onStudySettings = {
                        navController.navigate(
                            "study_settings"
                        )
                    },

                    onLogout =
                        onLogout
                )
            }
        }
    }
}


/*
 * PLACEHOLDER TEMPORAL
 *
 * Se utiliza para errores de navegación.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String
) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    28.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Text(
            text =
                title,

            fontSize =
                30.sp
        )


        Text(
            text =
                message,

            modifier =
                Modifier.padding(
                    top = 12.dp
                )
        )
    }
}


/*
 * PERFIL
 *
 * La pantalla de Perfil se define en este archivo para mantener
 * la estructura del proyecto indicada por el usuario. La tarjeta
 * de configuración forma parte del LazyColumn y se desplaza junto
 * al resto del contenido; ya no hay un botón flotante en navegación.
 */
private val ProfileBlue =
    Color(0xFF2563EB)

private val ProfileDark =
    Color(0xFF172033)

private val ProfileGray =
    Color(0xFF64748B)

private val ProfileBackground =
    Color(0xFFF4F7FB)

private val ProfileBorder =
    Color(0xFFE2E8F0)

private val ProfileRed =
    Color(0xFFDC2626)


@Composable
fun ProfileScreen(
    user: UserResponse,
    onStudySettings: () -> Unit,
    onLogout: () -> Unit
) {
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
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: "Usuario"
            }


    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ProfileBackground
                ),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 28.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {

        item {

            Text(
                text =
                    "Tu plan",
                color =
                    ProfileDark,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        item {

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
                            if (
                                user.isPremium
                            ) {
                                Color(
                                    0xFFFFF7E6
                                )
                            } else {
                                Color.White
                            }
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(
                        text =
                            if (
                                user.isPremium
                            ) {
                                "⭐"
                            } else {
                                "🚗"
                            },
                        fontSize =
                            30.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                14.dp
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
                                if (
                                    user.isPremium
                                ) {
                                    "VialApp Premium"
                                } else {
                                    "Plan gratuito"
                                },
                            color =
                                ProfileDark,
                            fontSize =
                                16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )


                        Text(
                            text =
                                if (
                                    user.isPremium
                                ) {
                                    "Tienes acceso a las funciones Premium disponibles para tu cuenta."
                                } else {
                                    "Puedes practicar con las funciones disponibles en tu plan."
                                },
                            color =
                                ProfileGray,
                            fontSize =
                                12.sp,
                            lineHeight =
                                17.sp
                        )
                    }
                }
            }
        }


        item {

            Text(
                text =
                    "Cuenta",
                color =
                    ProfileDark,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        item {

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
                            Color.White
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 17.dp,
                            vertical = 8.dp
                        )
                ) {

                    ProfileInfoRow(
                        emoji =
                            "👤",
                        label =
                            "Nombre",
                        value =
                            fullName
                    )


                    ProfileDivider()


                    ProfileInfoRow(
                        emoji =
                            "✉️",
                        label =
                            "Correo electrónico",
                        value =
                            user.email
                    )


                    ProfileDivider()


                    ProfileInfoRow(
                        emoji =
                            "🚘",
                        label =
                            "Clase de licencia",
                        value =
                            user.licenseClass
                                ?.let {
                                    "Clase $it"
                                }
                                ?: "No seleccionada"
                    )
                }
            }
        }


        item {

            Text(
                text =
                    "Configuración",
                color =
                    ProfileDark,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        item {

            Card(
                onClick =
                    onStudySettings,
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
                        Modifier.padding(
                            17.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    42.dp
                                )
                                .background(
                                    color =
                                        Color(
                                            0xFFEAF1FF
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            13.dp
                                        )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "⚙️",
                            fontSize =
                                20.sp
                        )
                    }


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
                                "Rutina de estudio",
                            color =
                                ProfileDark,
                            fontSize =
                                14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )


                        Text(
                            text =
                                "Meta diaria, intervalo y hora de recordatorios",
                            color =
                                ProfileGray,
                            fontSize =
                                11.sp
                        )
                    }


                    Text(
                        text =
                            "›",
                        color =
                            ProfileBlue,
                        fontSize =
                            26.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }


        item {

            Button(
                onClick =
                    onLogout,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.Transparent,
                        contentColor =
                            ProfileRed
                    ),
                border =
                    BorderStroke(
                        1.dp,
                        Color(
                            0xFFC7CDD8
                        )
                    )
            ) {

                Text(
                    text =
                        "Cerrar sesión",
                    color =
                        ProfileRed,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        item {

            Text(
                text =
                    "VialApp · Educación vial",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                            bottom = 4.dp
                        ),
                color =
                    Color(
                        0xFF94A3B8
                    ),
                fontSize =
                    11.sp
            )
        }
    }
}


@Composable
private fun ProfileInfoRow(
    emoji: String,
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 13.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        40.dp
                    )
                    .background(
                        color =
                            Color(
                                0xFFF3F6FA
                            ),
                        shape =
                            RoundedCornerShape(
                                13.dp
                            )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    emoji,
                fontSize =
                    18.sp
            )
        }


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
                    label,
                color =
                    ProfileGray,
                fontSize =
                    10.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )


            Text(
                text =
                    value,
                color =
                    ProfileDark,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun ProfileDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    1.dp
                )
                .background(
                    ProfileBorder
                )
    )
}
