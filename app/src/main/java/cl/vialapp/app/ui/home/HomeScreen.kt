package cl.vialapp.app.ui.home

import cl.vialapp.app.ui.rewards.LocalRewards
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cl.vialapp.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.AdvancedStatsResponse
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.ExamAttemptResponse
import cl.vialapp.app.data.api.UserResponse
import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.ui.learning.HomeJourneyCard
import cl.vialapp.app.ui.leaderboard.HomeLeagueShortcut
import cl.vialapp.app.data.api.WeeklyLeaderboardResponse
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.data.session.AuthenticatedRead
import cl.vialapp.app.data.session.readError
import cl.vialapp.app.ui.gamification.HomeXpCard
import cl.vialapp.app.ui.games.GamesQuickEntry
import cl.vialapp.app.ui.career.CareerShortcuts
import cl.vialapp.app.ui.profile.ProfileAvatar
import cl.vialapp.app.ui.theme.vialColor
import cl.vialapp.app.ui.theme.VialActionBlue
import cl.vialapp.app.ui.theme.LocalVialAppearance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val HomeBlue: Color
    @Composable get() = vialColor(Color(0xFF2563EB))
private val HomeDark: Color
    @Composable get() = vialColor(Color(0xFF172033))
private val HomeGray: Color
    @Composable get() = vialColor(Color(0xFF64748B))
private val HomeGreen: Color
    @Composable get() = vialColor(Color(0xFF14966F))
private val HomeOrange: Color
    @Composable get() = vialColor(Color(0xFFF59E0B))
private val HomeRed: Color
    @Composable get() = vialColor(Color(0xFFD92D20))
private val HomeCardBorder: BorderStroke
    @Composable get() = BorderStroke(1.dp, if (LocalVialAppearance.current.dark) Color(0xFF42536C) else Color(0xFFE2E8F0))
private val HomeBackground: Color
    @Composable get() = vialColor(Color(0xFFF4F7FB))

@Composable
fun HomeScreen(
    user: UserResponse,
    onPractice: () -> Unit,
    onExam: () -> Unit,
    onProgress: () -> Unit,
    onGames: () -> Unit,
    onResumeExam: (Int) -> Unit,
    avatarKey: String? = null,
    onLearningPath: () -> Unit = {},
    onCareer: () -> Unit = {},
    onGarage: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onSimulator: () -> Unit = {},
    onAdaptivePractice: () -> Unit = {},
    onContinueChapter: (String) -> Unit = { onLearningPath() },
    onOfflineStudy: () -> Unit = onLibrary,
    onLeague: () -> Unit = onProgress,
    onRally: () -> Unit = onGames
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val progressStore = remember(context, user.id) {
        GameProgressStore(
            context = context,
            userKey = user.id
        )
    }

    var stats by remember {
        mutableStateOf<AdvancedStatsResponse?>(null)
    }

    var activeAttempt by remember {
        mutableStateOf<ExamAttemptResponse?>(null)
    }

    val rewards=LocalRewards.current
    var wallet by remember(user.id){mutableStateOf<Int?>(null)}
    LaunchedEffect(user.id,rewards.serial){
        try {
            val response=AuthenticatedRead.get(sessionManager,"wallet"){ApiClient.api.getCareer(it)}
            if(response.isSuccessful)wallet=response.body()?.coins
            else response.errorBody()?.close()
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){}
    }
    var league by remember(user.id){mutableStateOf<WeeklyLeaderboardResponse?>(null)}
    LaunchedEffect(user.id,rewards.serial){
        try {
            val response=AuthenticatedRead.get(sessionManager,"weekly league preview"){ApiClient.api.getWeeklyLeaderboard(it,limit=1)}
            league=response.body()?.takeIf{response.isSuccessful}
            response.errorBody()?.close()
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){league=null}
    }
    var journey by remember(user.id) { mutableStateOf<List<LearningChapterSummary>?>(null) }
    var journeyLoading by remember(user.id) { mutableStateOf(true) }
    var journeyError by remember(user.id) { mutableStateOf<String?>(null) }
    var xpError by remember(user.id) { mutableStateOf<String?>(null) }
    var retrySync by remember(user.id) { mutableIntStateOf(0) }
    LaunchedEffect(user.id, user.licenseClass,retrySync) {
        journeyLoading = true
        journeyError = null
        try {
            val response=AuthenticatedRead.get(sessionManager,"learning"){ApiClient.api.getLearningPath(it)}
            val body=response.body()?.takeIf{response.isSuccessful}
            if(body!=null) journey=body.chapters.sortedBy{it.order}
            else journeyError=if(response.isSuccessful)"La respuesta del recorrido está vacía." else readError(response.code())
            response.errorBody()?.close()
        } catch(cancelled:CancellationException){throw cancelled}
        catch(e:Exception){journeyError=readError(e)}
        finally{journeyLoading=false}
    }

    var dashboardLoading by remember {
        mutableStateOf(true)
    }

    var xpLoaded by remember(user.id) {
        mutableStateOf(false)
    }

    var xpLoadFailed by remember(user.id) {
        mutableStateOf(false)
    }



    LaunchedEffect(user.id, user.licenseClass,retrySync) {

        dashboardLoading = true

        xpLoaded = false
        xpLoadFailed = false
        xpError = null

        val token = sessionManager.getAccessToken()

        if (token == null) {
            xpLoadFailed = true
            xpError = "Inicia sesión para continuar."
            dashboardLoading = false
            return@LaunchedEffect
        }


        // Independent reads are concurrent, isolated, and cancelled on navigation.
        coroutineScope {
            launch {
                try {
                    val response=AuthenticatedRead.get(sessionManager,"gamification"){ApiClient.api.getGamificationProgress(it)}
                    val remote=response.body()?.takeIf { response.isSuccessful }
                    if(remote!=null){progressStore.applyRemoteProgress(remote);xpLoaded=true}
                    else {xpLoadFailed=true;xpError=if(response.isSuccessful)"Respuesta de experiencia vacía." else readError(response.code())}
                    response.errorBody()?.close()
                } catch(e:CancellationException){throw e}
                catch(e:Exception){xpLoadFailed=true;xpError=readError(e)}
            }
            launch {
                try {
                    val response=AuthenticatedRead.get(sessionManager,"stats"){ApiClient.api.getAdvancedStats(it)}
                    if(response.isSuccessful)stats=response.body()
                } catch(e:CancellationException){throw e}
                catch(_:Exception){}
            }
            launch {
                try {
                    val response=AuthenticatedRead.get(sessionManager,"active exam"){ApiClient.api.getActiveAttempt(it)}
                    if(response.isSuccessful)activeAttempt=response.body()?.takeIf{it.status=="active"}
                } catch(e:CancellationException){throw e}
                catch(_:Exception){}
            }
        }
        dashboardLoading = false
    }

    val name = user.firstName?.takeIf { it.isNotBlank() } ?: "Conductor"
    val licenseText = user.licenseClass?.let {
        if (it.startsWith("Clase", ignoreCase = true)) it else "Clase $it"
    } ?: "Licencia"

    val summary = stats?.summary
    val priorityCategory = stats?.categories?.firstOrNull()

    val hasPremiumForLicense =
        user.isPremium ||
                user.premiumLicenseClasses.any { premiumClass ->
                    premiumClass.trim()
                        .removePrefix("Clase ")
                        .equals(
                            user.licenseClass
                                ?.trim()
                                ?.removePrefix("Clase "),
                            ignoreCase = true
                        )
                }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, $name",
                        color = HomeDark,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$licenseText · ${if (hasPremiumForLicense) "Premium" else "Cuenta gratuita"}",
                        color = HomeGray,
                        fontSize = 14.sp
                    )
                }

                ProfileAvatar(
                    name = name,
                    avatarKey = avatarKey,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // Compañero elegido: no modifica ni sincroniza el progreso.
        user.mascotId
            ?.takeIf { it == "carvier" || it == "mister_pelao" }
            ?.let { mascotId ->
                item {
                    HomeMascotCard(
                        mascotId = mascotId,
                        licenseClass = user.licenseClass
                    )
                }
            }

        // Examen en curso
        activeAttempt?.let { attempt ->
            item {
                Card(
                    onClick = { onResumeExam(attempt.testId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = vialColor(Color(0xFFEAF1FF)))
                ) {
                    Row(
                        modifier = Modifier.padding(19.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = vialColor(Color.White)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_vial_exam),null,tint=HomeBlue,modifier=Modifier.size(25.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Examen en curso",
                                color = HomeBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = attempt.test?.nombre ?: "Tienes un examen pendiente",
                                color = HomeDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Toca para continuar",
                                color = HomeGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }

                        Text(
                            text = "›",
                            color = HomeBlue,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }


        item(key="home_league") { HomeLeagueShortcut(league,onLeague) }

        item(key="home_journey") {
            HomeJourneyCard(user.id,licenseText,user.licenseClass,journey,journeyLoading,
                onPath=onLearningPath,onContinue=onContinueChapter,onExam=onExam,onPractice=onPractice,
                error=journeyError,onRetry={retrySync++})
        }

        item {
            when {
                xpLoaded -> {
                    HomeXpCard(
                        userKey = user.id,
                        onProgress = onProgress,
                        mascotId = user.mascotId
                    )
                }

                xpLoadFailed -> {
                    Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){
                        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                            Text("No se pudo sincronizar tu experiencia.",fontWeight=FontWeight.Bold,color=HomeDark)
                            Text(xpError?:"Comprueba tu conexión.",fontSize=13.sp,color=HomeGray)
                            TextButton(onClick={retrySync++}){Text("Reintentar sincronización")}
                        }
                    }
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = HomeBlue
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text(
                            text = "Cargando experiencia...",
                            color = HomeGray
                        )
                    }
                }
            }
        }


        item(key="wallet_shortcut"){
            val amount=wallet
            val dark=LocalVialAppearance.current.dark
            val gold=if(dark)Color(0xFFFFD77C)else Color(0xFF80520A)
            Card(onClick=onGarage,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(19.dp),
                colors=CardDefaults.cardColors(containerColor=if(dark)Color(0xFF3E331C)else Color(0xFFFFF2D6))){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Icon(painterResource(R.drawable.ic_vial_coin),null,tint=gold,modifier=Modifier.size(32.dp))
                    Column(Modifier.weight(1f)){
                        if(amount!=null){
                            val shown by androidx.compose.animation.core.animateIntAsState(amount,label="homeWallet")
                            Text("$shown monedas",color=gold,fontWeight=FontWeight.ExtraBold,fontSize=19.sp)
                        }else Text("Mis monedas",color=gold,fontWeight=FontWeight.Bold,fontSize=17.sp)
                        Text("Personaliza tu vehículo en el garaje",color=gold,fontSize=12.sp)
                    }
                    Text("›",color=gold,fontSize=26.sp)
                }
            }
        }

        // Recomendación de refuerzo
        item {
            Text(
                text = "Refuerzo recomendado",
                color = HomeDark,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = HomeCardBorder,
                colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (dashboardLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = HomeBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Preparando tu recomendación...", color = HomeGray)
                        }
                    } else if (priorityCategory != null) {
                        Text(
                            text = "Prioridad recomendada",
                            color = HomeBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = priorityCategory.categoria,
                            color = HomeDark,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "${priorityCategory.successPercentage}% de acierto · " +
                                    "${priorityCategory.incorrectas} errores registrados",
                            color = HomeGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(13.dp))
                        LinearProgressIndicator(
                            progress = {
                                (priorityCategory.successPercentage / 100f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = scoreColor(priorityCategory.successPercentage),
                            trackColor = vialColor(Color(0xFFE8EDF5))
                        )
                        Spacer(modifier = Modifier.height(17.dp))
                        Button(
                            onClick = onPractice,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VialActionBlue,
                                    contentColor = Color.White)
                        ) {
                            Text(text = "Practicar ahora", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Descubre tus puntos débiles",
                            color = HomeDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Completa un examen y VialApp podrá recomendarte qué temas reforzar.",
                            color = HomeGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExam,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(text = "Hacer un examen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("Herramientas de estudio",color=HomeDark,fontSize=21.sp,fontWeight=FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                HomeActionCard(Modifier.weight(1f).fillMaxHeight(),R.drawable.ic_vial_library,"Biblioteca vial","Consulta sin conexión",onLibrary)
                HomeActionCard(Modifier.weight(1f).fillMaxHeight(),R.drawable.ic_vial_bookmark,"Repaso sin conexión","Practica donde estés",onOfflineStudy)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                HomeActionCard(Modifier.weight(1f).fillMaxHeight(),R.drawable.ic_award_wheel,"Simulador vial","Situaciones de tránsito",onSimulator)
                HomeActionCard(Modifier.weight(1f).fillMaxHeight(),R.drawable.ic_award_target,"Práctica inteligente","Refuerza tus errores",onAdaptivePractice)
            }
        }
        // Resumen de progreso
        if (summary?.hasData == true) {
            item {
                Text(
                    text = "Tu progreso",
                    color = HomeDark,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    onClick = onProgress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = HomeCardBorder,
                colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.averageScore}%",
                                label = "Promedio"
                            )
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.totalTests}",
                                label = "Exámenes"
                            )
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.totalCorrectAnswers}",
                                label = "Correctas"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ver progreso completo",
                                modifier = Modifier.weight(1f),
                                color = HomeBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "›",
                                color = HomeBlue,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Aprende jugando",color=HomeDark,fontSize=21.sp,fontWeight=FontWeight.Bold)
        }
        item { GamesQuickEntry(onClick=onGames) }
        item { cl.vialapp.app.ui.games.RallyQuickEntry(onClick=onRally) }
        item { CareerShortcuts(onCareer,onGarage) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (LocalVialAppearance.current.dark) Color(0xFF6C5432) else Color(0xFFF0DDB4)),
                colors = CardDefaults.cardColors(containerColor = vialColor(Color(0xFFFFF7E6)))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeFeatureIcon(R.drawable.ic_home_growth, if (LocalVialAppearance.current.dark) Color(0xFFFCD38D) else Color(0xFF995C08), if (LocalVialAppearance.current.dark) Color(0xFF60492B) else Color(0xFFFFE8B6))
                    Spacer(modifier = Modifier.width(15.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Un poco cada día",
                            color = HomeDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Practicar con constancia te ayuda a detectar y reforzar tus errores antes del examen.",
                            color = HomeGray,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (LocalVialAppearance.current.dark) Color(0xFF2A394F) else Color(0xFFF4F7FB)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = HomeDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = HomeGray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HomeActionCard(
    modifier: Modifier,
    drawable: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min=112.dp),
        shape = RoundedCornerShape(22.dp),
        border = HomeCardBorder,
        colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(painterResource(drawable),null,tint=MaterialTheme.colorScheme.onPrimaryContainer,modifier=Modifier.padding(9.dp).size(23.dp))
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(text = title, color = HomeDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = description, color = HomeGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun scoreColor(score: Int): Color {
    return when {
        score >= 80 -> HomeGreen
        score >= 60 -> HomeOrange
        else -> HomeRed
    }
}
@Composable
private fun HomeFeatureIcon(drawable: Int, tint: Color, background: Color) {
    Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(drawable), contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        }
    }
}
