package com.github.rodrigotimoteo.kboyemu.domain.savegame

import android.content.Context
import androidx.core.net.toUri
import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemu.util.md5Hex
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single

/**
 * Default implementation of [SaveGameRepository] that persists ERAM dumps as .sav files in
 * internal storage. Each file is named after an MD5 hash of the ROM content.
 *
 * @author rodrigotimoteo
 */
@Single
class SaveGameRepositoryImpl(
    private val context: Context,
    private val logger: Logger,
) : SaveGameRepository {

    /** MD5 hash of the currently loaded ROM, set via [setRomHash] */
    private var romHash: String? = null

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setRomHash(romBytes: UByteArray) {
        romHash = md5Hex(romBytes)
    }

    override fun save(data: ByteArray): Boolean {
        val hash = romHash ?: run {
            logger.i("Cannot save game — no ROM loaded")
            return false
        }

        return try {
            val file = context.filesDir.resolve("savegame_$hash.sav")
            file.writeBytes(data)
            logger.i("Save game written (${file.length()} bytes)")
            true
        } catch (e: Exception) {
            logger.e("Failed to write save game", e)
            false
        }
    }

    override fun load(): ByteArray? {
        val hash = romHash ?: run {
            logger.i("Cannot load game — no ROM loaded")
            return null
        }

        return try {
            val file = context.filesDir.resolve("savegame_$hash.sav")
            if (!file.exists()) {
                logger.i("No save game found")
                return null
            }
            val data = file.readBytes()
            logger.i("Save game loaded (${data.size} bytes)")
            data
        } catch (e: Exception) {
            logger.e("Failed to load save game", e)
            null
        }
    }

    override fun importFrom(uri: String): ByteArray? {
        return try {
            val parsedUri = uri.toUri()
            context.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            logger.e("Failed to import save game from URI: $uri", e)
            null
        }
    }

    override fun exportTo(uri: String, data: ByteArray): Boolean {
        return try {
            val parsedUri = uri.toUri()
            context.contentResolver.openOutputStream(parsedUri)?.use { it.write(data) }
            logger.i("Save game exported (${data.size} bytes)")
            true
        } catch (e: Exception) {
            logger.e("Failed to export save game to URI: $uri", e)
            false
        }
    }
}

