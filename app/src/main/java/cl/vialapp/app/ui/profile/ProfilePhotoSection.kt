package cl.vialapp.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ProfilePhotoSection(
    name: String,
    avatarKey: String?,
    enabled: Boolean,
    onAvatarChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onChanged by rememberUpdatedState(onAvatarChanged)
    var options by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !busy) {
            busy = true
            error = null
            scope.launch {
                try {
                    val token = SessionManager(context).getAccessToken()
                    if (token.isNullOrBlank()) {
                        error = "Tu sesión expiró."
                    } else {
                        val bytes = withContext(Dispatchers.IO) { prepareAvatar(context, uri) }
                        val response = ApiClient.api.putMyAvatar("Bearer $token", bytes.toRequestBody("image/jpeg".toMediaType()))
                        val key = response.body()?.key
                        if (response.isSuccessful && !key.isNullOrBlank()) {
                            onChanged(key)
                        } else {
                            response.errorBody()?.close()
                            error = "No se pudo guardar la foto. Inténtalo de nuevo."
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "No se pudo abrir o subir esta imagen."
                } finally {
                    busy = false
                }
            }
        }
    }

    if (options) {
        AlertDialog(
            onDismissRequest = { options = false },
            title = { Text("Foto de perfil") },
            text = {
                Text("La foto se recortará al centro y se verá en tu perfil y en la liga semanal para otros usuarios de VialApp.")
            },
            confirmButton = {
                TextButton(onClick = { options = false; picker.launch("image/*") }) {
                    Text(if (avatarKey == null) "Elegir foto" else "Cambiar foto")
                }
            },
            dismissButton = {
                if (avatarKey != null) {
                    TextButton(onClick = {
                        options = false
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val token = SessionManager(context).getAccessToken()
                                if (token.isNullOrBlank()) {
                                    error = "Tu sesión expiró."
                                } else {
                                    val response = ApiClient.api.deleteMyAvatar("Bearer $token")
                                    if (response.isSuccessful) onChanged(null)
                                    else {
                                        response.errorBody()?.close()
                                        error = "No se pudo eliminar la foto."
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = "No hay conexión con el servidor."
                            } finally {
                                busy = false
                            }
                        }
                    }) { Text("Eliminar foto", color = Color(0xFFDC2626)) }
                } else {
                    TextButton(onClick = { options = false }) { Text("Cancelar") }
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatar(name, avatarKey, Modifier.size(88.dp).clickable(enabled = enabled && !busy) { options = true })
            Spacer(Modifier.height(9.dp))
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
            if (busy) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                TextButton(enabled = enabled, onClick = { options = true }) {
                    Text(if (avatarKey == null) "Añadir foto de perfil" else "Cambiar foto de perfil")
                }
            }
            error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFDC2626)) }
        }
    }
}
