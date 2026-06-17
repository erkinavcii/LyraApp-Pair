package com.turkcell.lyraapp.ui.library.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.library.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewPlaylistViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPlaylistUiState())
    val uiState: StateFlow<NewPlaylistUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NewPlaylistEffect>(Channel.BUFFERED)
    val effect: Flow<NewPlaylistEffect> = _effect.receiveAsFlow()

    init {
        loadAvailableTracks()
    }

    fun onIntent(intent: NewPlaylistIntent) {
        when (intent) {
            is NewPlaylistIntent.NameChanged -> updateState { it.copy(name = intent.value) }
            is NewPlaylistIntent.DescriptionChanged -> updateState { it.copy(description = intent.value) }
            is NewPlaylistIntent.PublicToggled -> updateState { it.copy(isPublic = intent.value) }
            is NewPlaylistIntent.ToggleTrackSelection -> toggleTrackSelection(intent.trackId)
            NewPlaylistIntent.SavePlaylist -> savePlaylist()
            NewPlaylistIntent.NavigateBack -> {
                viewModelScope.launch {
                    _effect.send(NewPlaylistEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadAvailableTracks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            libraryRepository.getAvailableTracks()
                .onSuccess { tracks ->
                    _uiState.update { it.copy(isLoading = false, availableTracks = tracks) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(NewPlaylistEffect.ShowError(error.message ?: "Şarkı listesi yüklenemedi."))
                }
        }
    }

    private fun toggleTrackSelection(trackId: String) {
        updateState { current ->
            val updatedSelection = current.selectedTrackIds.toMutableSet()
            if (updatedSelection.contains(trackId)) {
                updatedSelection.remove(trackId)
            } else {
                updatedSelection.add(trackId)
            }
            current.copy(selectedTrackIds = updatedSelection)
        }
    }

    private fun savePlaylist() {
        val state = _uiState.value
        if (state.isLoading || !state.isSaveEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Seçilen şarkı ID'lerini nesnelere eşleme
            val selectedTracks = state.availableTracks.filter { state.selectedTrackIds.contains(it.id) }

            libraryRepository.createPlaylist(
                name = state.name,
                description = state.description,
                isPublic = state.isPublic,
                tracks = selectedTracks
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(NewPlaylistEffect.NavigateBack)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(NewPlaylistEffect.ShowError(error.message ?: "Çalma listesi oluşturulamadı."))
            }
        }
    }

    private fun updateState(transform: (NewPlaylistUiState) -> NewPlaylistUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(
                isSaveEnabled = updated.name.isNotBlank()
            )
        }
    }
}
