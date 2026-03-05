package com.github.rodrigotimoteo.kboyemu.domain.savestate

import android.content.Context
import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemu.util.md5Hex
import com.github.rodrigotimoteo.kboyemucore.api.SaveState
import com.github.rodrigotimoteo.kboyemucore.api.SaveState.Companion.toByteArray
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single

/**
 * Default implementation of [com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository] that persists save states as files in internal
 * storage. Each file is named after an MD5 hash of the ROM to keep different games independent.
 *
 * @author rodrigotimoteo
 */
@Single
class SaveStateRepositoryImpl(
    private val context: Context,
    private val logger: Logger,
) : SaveStateRepository {

    /** MD5 hash of the currently loaded ROM, set via [setRomHash] */
    private var romHash: String? = null

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setRomHash(romBytes: UByteArray) {
        romHash = md5Hex(romBytes)
    }

    override fun save(state: SaveState): Boolean {
        val hash = romHash ?: run {
            logger.i("Cannot save state — no ROM loaded")
            return false
        }

        return try {
            val file = context.filesDir.resolve("savestate_$hash.bin")
            file.writeBytes(state.toByteArray())
            logger.i("Save state written (${file.length()} bytes)")
            true
        } catch (e: Exception) {
            logger.e("Failed to write save state", e)
            false
        }
    }

    override fun load(): SaveState? {
        val hash = romHash ?: run {
            logger.i("Cannot load state — no ROM loaded")
            return null
        }

        return try {
            val file = context.filesDir.resolve("savestate_$hash.bin")
            if (!file.exists()) {
                logger.i("No save state found")
                return null
            }
            val state = SaveState.Companion.fromByteArray(file.readBytes())
            logger.i("Save state loaded")
            state
        } catch (e: Exception) {
            logger.e("Failed to load save state", e)
            null
        }
    }
}