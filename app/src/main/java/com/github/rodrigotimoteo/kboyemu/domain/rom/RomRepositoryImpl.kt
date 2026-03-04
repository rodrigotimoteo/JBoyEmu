package com.github.rodrigotimoteo.kboyemu.domain.rom

import android.content.Context
import androidx.core.net.toUri
import com.github.rodrigotimoteo.kboyemu.data.rom.RomRepository
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single

/**
 * Default [com.github.rodrigotimoteo.kboyemu.data.rom.RomRepository] implementation that reads ROM bytes from Android content URIs
 * using the application [android.content.Context].
 *
 * @author rodrigotimoteo
 */
@Single
class RomRepositoryImpl(
    private val context: Context,
    private val logger: Logger,
) : RomRepository {

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun loadFromUri(uri: String): UByteArray? {
        return try {
            val parsedUri = uri.toUri()
            context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                stream.readBytes().toUByteArray()
            }
        } catch (e: Exception) {
            logger.e("Failed to read ROM from URI: $uri", e)
            null
        }
    }
}