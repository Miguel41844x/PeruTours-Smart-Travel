package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AgentHomeViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentHomeUiState())
    val uiState: StateFlow<AgentHomeUiState> = _uiState.asStateFlow()

    init {
        loadAgentData()
        observeTravelRequests()
    }

    private fun loadAgentData() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid.orEmpty()
        val rawName = currentUser?.displayName.orEmpty()
        val initialName = rawName.substringBefore("|").trim().takeIf(String::isNotBlank)
            ?: currentUser?.email?.substringBefore("@")
            ?: "Agente"
        val initialPhoto = currentUser?.photoUrl?.toString().orEmpty()

        _uiState.update {
            it.copy(
                agentName = initialName,
                photoUrl = initialPhoto
            )
        }

        if (uid.isNotBlank()) {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot?.exists() == true) {
                        val name = snapshot.getString("name")?.takeIf(String::isNotBlank)
                        val photo = snapshot.getString("photoUrl")
                        _uiState.update { state ->
                            state.copy(
                                agentName = name ?: state.agentName,
                                photoUrl = photo ?: state.photoUrl
                            )
                        }
                    }
                }
        }
    }

    private fun observeTravelRequests() {
        firestore.collection("travel_requests")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val pending = snapshot.documents.count { document ->
                        document.getString("status").orEmpty().contains("Pendiente", ignoreCase = true)
                    }
                    val quoted = snapshot.documents.count { document ->
                        document.getString("status").orEmpty().equals("Cotizado", ignoreCase = true)
                    }
                    _uiState.update {
                        it.copy(
                            pendingCount = pending,
                            quotedCount = quoted,
                            isLoadingCounts = false
                        )
                    }
                }
            }
    }
}
