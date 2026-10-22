package cl.vialapp.app.ui.exams

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cl.vialapp.app.data.api.AssignedTest
import cl.vialapp.app.data.api.ExamAttemptResponse
import cl.vialapp.app.data.session.SessionManager


private val VialBlue = Color(0xFF2563EB)
private val VialDark = Color(0xFF172033)
private val VialGray = Color(0xFF64748B)


@Composable
fun ExamsScreen(
    licenseClass: String?,
    category: String? = null,
    onExamClick: (Int) -> Unit,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }


    val context =
        LocalContext.current


    val sessionManager =
        remember {
            SessionManager(context)
        }


    var tests by remember {
        mutableStateOf<List<AssignedTest>>(
            emptyList()
        )
    }


    var loading by remember {
        mutableStateOf(true)
    }


    var error by remember {
        mutableStateOf("")
    }


    var activeAttempt by remember {
        mutableStateOf<ExamAttemptResponse?>(
            null
        )
    }


    var showActiveExamDialog by remember {
        mutableStateOf(false)
    }


    /*
     * Carga los exámenes.
     *
     * Si category es null:
     * carga los exámenes normales.
     *
     * Si category tiene un valor:
     * filtra por esa categoría.
     */
    LaunchedEffect(
        licenseClass,
        category
    ) {

        loading = true
        error = ""


        try {

            val token =
                sessionManager.getAccessToken()


            if (token == null) {

                error =
                    "No se encontró una sesión válida"

                return@LaunchedEffect
            }


            /*
             * CARGAR EXÁMENES
             */
            val testsResponse =
                ApiClient.api.getTests(
                    authorization =
                        "Bearer $token",

                    licenseClass =
                        licenseClass,

                    category =
                        category
                )


            if (testsResponse.isSuccessful) {

                tests =
                    testsResponse.body()
                        ?.tests
                        ?: emptyList()

            } else {

                error =
                    "No se pudieron cargar los exámenes"

                return@LaunchedEffect
            }


            /*
             * COMPROBAR SI HAY
             * UN EXAMEN ACTIVO
             */
            val activeResponse =
                ApiClient.api.getActiveAttempt(
                    authorization =
                        "Bearer $token"
                )


            if (activeResponse.isSuccessful) {

                val currentAttempt =
                    activeResponse.body()


                activeAttempt =
                    if (
                        currentAttempt?.status ==
                        "active"
                    ) {

                        currentAttempt

                    } else {

                        null
                    }
            }


        } catch (e: Exception) {

            error =
                "No se pudo conectar con VialApp"

            e.printStackTrace()


        } finally {

            loading = false
        }
    }


    /*
     * Intentamos encontrar el examen activo
     * dentro de la lista actual.
     *
     * Si estamos filtrando por una categoría
     * diferente, puede que no esté en esta lista.
     */
    val activeTest =
        tests.firstOrNull {

            it.id ==
                    activeAttempt?.testId
        }


    when {

        loading -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFFF7F9FC)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color =
                        VialBlue
                )
            }
        }


        error.isNotEmpty() -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFFF7F9FC)
                        )
                        .padding(
                            24.dp
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        error,

                    color =
                        Color(0xFFDC2626)
                )
            }
        }


        else -> {

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFFF7F9FC)
                        )
                        .padding(
                            horizontal = 20.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    ),

                contentPadding =
                    PaddingValues(
                        top = 24.dp,
                        bottom = 24.dp
                    )
            ) {


                /*
                 * CABECERA
                 */
                item {

                    Text(
                        text =
                            if (
                                category.isNullOrBlank()
                            ) {

                                "Tus exámenes"

                            } else {

                                category
                            },

                        color =
                            VialDark,

                        fontSize =
                            30.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            if (
                                category.isNullOrBlank()
                            ) {

                                "Practica con exámenes asignados para tu licencia."

                            } else {

                                "Exámenes para reforzar esta categoría."
                            },

                        color =
                            VialGray,

                        fontSize =
                            15.sp
                    )


                    /*
                     * AVISO DE EXAMEN ACTIVO
                     */
                    if (
                        activeAttempt != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    18.dp
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
                                        Color(
                                            0xFFEAF1FF
                                        )
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        18.dp
                                    )
                            ) {


                                Text(
                                    text =
                                        "⏱ Examen en curso",

                                    color =
                                        VialBlue,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        16.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            5.dp
                                        )
                                )


                                Text(
                                    text =
                                        activeTest
                                            ?.nombre
                                            ?.let {
                                                cleanExamTitle(
                                                    it
                                                )
                                            }
                                            ?: "Tienes un examen sin finalizar.",

                                    color =
                                        VialDark,

                                    fontSize =
                                        14.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            12.dp
                                        )
                                )


                                Button(
                                    onClick = {

                                        activeAttempt
                                            ?.testId
                                            ?.let { testId ->

                                                onExamClick(
                                                    testId
                                                )
                                            }
                                    },

                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                VialBlue
                                        )
                                ) {

                                    Text(
                                        "Continuar examen"
                                    )
                                }
                            }
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )
                }


                /*
                 * SIN EXÁMENES
                 */
                if (
                    tests.isEmpty()
                ) {

                    item {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(
                                    24.dp
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
                                        24.dp
                                    )
                            ) {


                                Text(
                                    text =
                                        "🚗",

                                    fontSize =
                                        36.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            12.dp
                                        )
                                )


                                Text(
                                    text =
                                        if (
                                            category.isNullOrBlank()
                                        ) {

                                            "No hay exámenes disponibles"

                                        } else {

                                            "No hay exámenes para esta categoría"
                                        },

                                    color =
                                        VialDark,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        18.sp
                                )


                                Text(
                                    text =
                                        if (
                                            category.isNullOrBlank()
                                        ) {

                                            "Cuando tengas nuevos exámenes asignados aparecerán aquí."

                                        } else {

                                            "Por ahora no tienes un examen disponible para reforzar $category."
                                        },

                                    color =
                                        VialGray,

                                    modifier =
                                        Modifier.padding(
                                            top = 6.dp
                                        )
                                )
                            }
                        }
                    }


                } else {


                    /*
                     * LISTA
                     */
                    items(
                        items =
                            tests,

                        key = {
                            "${it.id}-${it.assignedDate}"
                        }
                    ) { test ->


                        val isActive =
                            activeAttempt?.status ==
                                    "active" &&
                                    activeAttempt?.testId ==
                                    test.id


                        ExamCard(
                            test =
                                test,

                            isActive =
                                isActive,

                            onClick = {


                                /*
                                 * ESTE MISMO EXAMEN
                                 * YA ESTÁ ACTIVO.
                                 */
                                if (isActive) {

                                    onExamClick(
                                        test.id
                                    )

                                    return@ExamCard
                                }


                                /*
                                 * HAY OTRO EXAMEN
                                 * ACTIVO.
                                 */
                                if (
                                    activeAttempt != null
                                ) {

                                    showActiveExamDialog =
                                        true

                                    return@ExamCard
                                }


                                /*
                                 * NO HAY NINGÚN
                                 * INTENTO ACTIVO.
                                 */
                                if (
                                    test.canStart
                                ) {

                                    onExamClick(
                                        test.id
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }


    /*
     * AVISO SI EL USUARIO INTENTA
     * ABRIR OTRO EXAMEN.
     */
    if (
        showActiveExamDialog &&
        activeAttempt != null
    ) {

        AlertDialog(

            onDismissRequest = {

                showActiveExamDialog =
                    false
            },

            title = {

                Text(
                    text =
                        "Tienes un examen en curso"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Solo puedes tener un examen cronometrado activo a la vez."
                    )


                    if (
                        activeTest != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Text(
                            text =
                                cleanExamTitle(
                                    activeTest.nombre
                                ),

                            color =
                                VialDark,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Text(
                        text =
                            "Continúa ese examen o finalízalo antes de comenzar otro."
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        val activeTestId =
                            activeAttempt?.testId


                        showActiveExamDialog =
                            false


                        if (
                            activeTestId != null
                        ) {

                            onExamClick(
                                activeTestId
                            )
                        }
                    }
                ) {

                    Text(
                        "Continuar examen"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        showActiveExamDialog =
                            false
                    }
                ) {

                    Text(
                        "Cancelar"
                    )
                }
            }
        )
    }
}


@Composable
private fun ExamCard(
    test: AssignedTest,
    isActive: Boolean,
    onClick: () -> Unit
) {

    val statusText =
        when {

            isActive ->
                "En curso"

            test.isLocked ->
                "Premium"

            test.hasResult ->
                "Completado"

            test.canStart ->
                "Disponible"

            else ->
                "No disponible"
        }


    val statusColor =
        when {

            isActive ->
                VialBlue

            test.isLocked ->
                Color(0xFFF59E0B)

            test.hasResult ->
                Color(0xFF16A34A)

            else ->
                VialBlue
        }


    /*
     * Un examen activo siempre
     * debe poder abrirse.
     */
    val cardEnabled =
        test.canStart ||
                isActive


    Card(
        onClick =
            onClick,

        enabled =
            cardEnabled,

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isActive) {

                        Color(
                            0xFFF5F9FF
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
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Box(
                    modifier =
                        Modifier
                            .size(
                                48.dp
                            )
                            .background(
                                color =
                                    if (isActive) {

                                        Color(
                                            0xFFDCE9FF
                                        )

                                    } else {

                                        Color(
                                            0xFFE8F0FF
                                        )
                                    },

                                shape =
                                    RoundedCornerShape(
                                        15.dp
                                    )
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (isActive) {
                                "⏱"
                            } else {
                                "🚘"
                            },

                        fontSize =
                            24.sp
                    )
                }


                Column(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .padding(
                                start = 14.dp
                            )
                ) {

                    Text(
                        text =
                            cleanExamTitle(
                                test.nombre
                            ),

                        color =
                            VialDark,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            17.sp
                    )


                    Text(
                        text =
                            test.categoria
                                ?: "Examen general",

                        color =
                            VialGray,

                        fontSize =
                            14.sp,

                        modifier =
                            Modifier.padding(
                                top = 3.dp
                            )
                    )
                }


                Text(
                    text =
                        statusText,

                    color =
                        statusColor,

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            /*
             * RAZÓN DEL BLOQUEO
             */
            if (
                test.isLocked &&
                test.lockedReason != null
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                Text(
                    text =
                        "🔒 ${test.lockedReason}",

                    color =
                        Color(
                            0xFF92400E
                        ),

                    fontSize =
                        13.sp
                )
            }


            /*
             * BOTÓN
             */
            if (
                cardEnabled
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                Button(
                    onClick =
                        onClick,

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                VialBlue
                        )
                ) {

                    Text(
                        text =
                            when {

                                isActive ->
                                    "Continuar examen"

                                test.hasResult ->
                                    "Volver a realizar"

                                else ->
                                    "Comenzar examen"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


private fun cleanExamTitle(
    name: String
): String {

    return name
        .replace(
            Regex(
                "\\s\\d{8}-\\d{4}$"
            ),
            ""
        )
        .trim()
}