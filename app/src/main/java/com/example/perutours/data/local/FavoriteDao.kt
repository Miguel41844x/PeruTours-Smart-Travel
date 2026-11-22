package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY savedAtMillis DESC, title ASC")
    abstract fun observeAll(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND itemId = :id AND kind = :kind LIMIT 1")
    abstract suspend fun get(userId: String, id: String, kind: String): FavoriteEntity?

    @Upsert
    abstract suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND itemId = :id AND kind = :kind")
    abstract suspend fun remove(userId: String, id: String, kind: String)

    /** Una sola transacción evita duplicados ante dos toques seguidos. */
    @Transaction
    open suspend fun toggle(item: FavoriteEntity): Boolean {
        if (get(item.userId, item.itemId, item.kind) != null) {
            remove(item.userId, item.itemId, item.kind)
            return false
        }
        upsert(item)
        return true
    }
}
