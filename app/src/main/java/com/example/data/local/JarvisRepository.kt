package com.example.data.local

import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val noteDao: NoteDao,
    private val memoryDao: MemoryDao,
    private val chatMessageDao: ChatMessageDao
) {
    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun getNotesList(): List<NoteEntity> = noteDao.getNotesList()
    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
    suspend fun clearNotes() = noteDao.clearAllNotes()

    // Memories
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    suspend fun getMemoriesList(): List<MemoryEntity> = memoryDao.getMemoriesList()
    suspend fun insertMemory(category: String, key: String, value: String): Long {
        return memoryDao.insertMemory(
            MemoryEntity(category = category, memoryKey = key, memoryValue = value)
        )
    }
    suspend fun deleteMemoryById(id: Long) = memoryDao.deleteMemoryById(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    // Chat History
    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessageEntity> =
        chatMessageDao.getRecentMessages(limit)
    suspend fun insertMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insertMessage(message)
    suspend fun clearHistory() = chatMessageDao.clearHistory()
}
