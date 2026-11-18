package com.example.data.local

import com.example.domain.model.AssistantLanguage
import com.example.domain.model.CallCommandIntent
import com.example.domain.model.CommandSource
import kotlinx.coroutines.flow.Flow

class CommandHistoryRepository(private val dao: CommandHistoryDao) {
    val allHistory: Flow<List<CommandHistoryEntity>> = dao.observeAll()

    fun recentHistory(limit: Int = 5): Flow<List<CommandHistoryEntity>> = dao.observeRecent(limit)

    suspend fun recordCommand(
        spokenText: String,
        intent: CallCommandIntent,
        confidence: Float,
        source: CommandSource,
        outcomeMessage: String,
        isSuccess: Boolean,
        language: AssistantLanguage
    ) {
        dao.insert(
            CommandHistoryEntity(
                spokenText = spokenText,
                intentName = intent.name,
                intentLabel = intent.label,
                confidence = confidence,
                sourceName = source.name,
                outcomeMessage = outcomeMessage,
                isSuccess = isSuccess,
                languageName = language.displayName
            )
        )
    }

    suspend fun deleteEntry(id: Long) = dao.deleteById(id)

    suspend fun clearHistory() = dao.clearAll()
}
