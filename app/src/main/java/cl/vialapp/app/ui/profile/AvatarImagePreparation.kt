package cl.vialapp.app.ui.profile

// GUÍA DEL ARCHIVO:
// Prepara fotos sin cargar archivos enormes en memoria sin necesidad.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

// Bound the input before decoding, then send only a small, square JPEG.
internal fun prepareAvatar(context: Context, uri: Uri): ByteArray {
    val maxBytes = 15 * 1024 * 1024
    val source = context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            require(out.size() + size <= maxBytes) { "El archivo supera los 15 MB." }
            out.write(buffer, 0, size)
        }
        out.toByteArray()
    } ?: error("No se pudo abrir la imagen.")

    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source))) { decoder, info, _ ->
            val size = info.size
            val scale = minOf(1f, 640f / max(size.width, size.height).coerceAtLeast(1))
            decoder.setTargetSize((size.width * scale).roundToInt().coerceAtLeast(1), (size.height * scale).roundToInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagen no válida." }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 640) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Imagen no válida.")
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(source)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }
        if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }
    val side = minOf(bitmap.width, bitmap.height)
    val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
    val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
    return ByteArrayOutputStream().use { output ->
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)) { "No se pudo preparar la foto." }
        output.toByteArray()
    }
}
