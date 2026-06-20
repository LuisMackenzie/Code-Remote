package dev.mackenzie.coderemote.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mackenzie.coderemote.data.FileStorage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileStorage {

    override fun readText(fileName: String): String? {
        return File(context.filesDir, fileName).takeIf { it.exists() }?.readText()
    }

    override fun writeText(fileName: String, content: String) {
        File(context.filesDir, fileName).writeText(content)
    }
}
