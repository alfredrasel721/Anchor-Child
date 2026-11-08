package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pairingDataStore by preferencesDataStore(name = "anchor_child_pairing_session")

data class PersistedPairingSession(
    val isPaired: Boolean = false,
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "child_leo",
    val childName: String = "Leo",
    val codeId: String = "",
    val pairedAtEpochMs: Long = 0L
)

class PairingSessionStore(private val context: Context) {

    val sessionFlow: Flow<PersistedPairingSession> = context.pairingDataStore.data.map { prefs ->
        PersistedPairingSession(
            isPaired = prefs[KEY_IS_PAIRED] ?: false,
            familyId = prefs[KEY_FAMILY_ID] ?: "",
            ownerUid = prefs[KEY_OWNER_UID] ?: "",
            childId = prefs[KEY_CHILD_ID] ?: "child_leo",
            childName = prefs[KEY_CHILD_NAME] ?: "Leo",
            codeId = prefs[KEY_CODE_ID] ?: "",
            pairedAtEpochMs = prefs[KEY_PAIRED_AT] ?: 0L
        )
    }

    suspend fun saveVerifiedPairing(
        familyId: String,
        ownerUid: String,
        childId: String,
        childName: String,
        codeId: String
    ) {
        context.pairingDataStore.edit { prefs ->
            prefs[KEY_IS_PAIRED] = true
            prefs[KEY_FAMILY_ID] = familyId
            prefs[KEY_OWNER_UID] = ownerUid
            prefs[KEY_CHILD_ID] = childId
            prefs[KEY_CHILD_NAME] = childName
            prefs[KEY_CODE_ID] = codeId
            prefs[KEY_PAIRED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clearPairingSession() {
        context.pairingDataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_IS_PAIRED = booleanPreferencesKey("is_paired")
        private val KEY_FAMILY_ID = stringPreferencesKey("family_id")
        private val KEY_OWNER_UID = stringPreferencesKey("owner_uid")
        private val KEY_CHILD_ID = stringPreferencesKey("child_id")
        private val KEY_CHILD_NAME = stringPreferencesKey("child_name")
        private val KEY_CODE_ID = stringPreferencesKey("code_id")
        private val KEY_PAIRED_AT = longPreferencesKey("paired_at_epoch_ms")
    }
}
