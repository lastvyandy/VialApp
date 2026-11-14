package cl.vialapp.app.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cl.vialapp.app.ui.theme.vialColor

// In-memory thumbnails only, scoped by authenticated session. No disk cache of personal photos.
private val avatarCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

internal fun profileInitials(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("").ifBlank { "V" }

@Composable
fun ProfileAvatar(
    name: String,
    avatarKey: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val token = SessionManager(context).getAccessToken()
    key(token, avatarKey) {
        val cacheKey = if (!token.isNullOrBlank() && !avatarKey.isNullOrBlank()) "$token:$avatarKey" else null
        val bitmap by produceState<Bitmap?>(cacheKey?.let { avatarCache.get(it) }, cacheKey) {
            if (cacheKey != null && value == null && token != null && avatarKey != null) {
                try {
                    value = withContext(Dispatchers.IO) {
                        val response = ApiClient.api.getAvatar("Bearer $token", avatarKey)
                        if (!response.isSuccessful) {
                            response.errorBody()?.close()
                            null
                        } else {
                            response.body()?.use { body ->
                                val bytes = body.bytes()
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }?.also { avatarCache.put(cacheKey, it) }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Show initials if a photo was removed or the network is unavailable.
                }
            }
        }
        val photo = bitmap
        Box(
            modifier = modifier.clip(CircleShape).background(vialColor(Color(0xFFE8F0FF))),
            contentAlignment = Alignment.Center
        ) {
            if (photo != null) {
                val image = remember(photo) { photo.asImageBitmap() }
                Image(image, contentDescription = "Foto de perfil de $name", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(profileInitials(name), color = vialColor(Color(0xFF2563EB)), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
