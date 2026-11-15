package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.PriceChartingScraper
import com.example.data.model.GameItem
import com.example.data.pricecharting.PriceChartingPreferenceManager
import com.example.data.repository.GameRepository
import com.example.worker.MonthlyPriceUpdateWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOrder(val label: String) {
    TITLE_ASC("Titre (A-Z)"),
    VALUE_DESC("Valeur (€) ↓"),
    RATING_DESC("Note (★) ↓"),
    YEAR_DESC("Année de sortie ↓"),
    RECENT_ADDED("Derniers ajouts")
}

class GameDexViewModel(private val repository: GameRepository) : ViewModel() {

    // Tab Selection: 0 = Collection, 1 = Wishlist, 2 = Statistiques
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlatform = MutableStateFlow("Toutes")
    val selectedPlatform: StateFlow<String> = _selectedPlatform.asStateFlow()

    private val _selectedCompleteness = MutableStateFlow("Toutes")
    val selectedCompleteness: StateFlow<String> = _selectedCompleteness.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Dialog & Detail states
    private val _selectedGameForDetail = MutableStateFlow<GameItem?>(null)
    val selectedGameForDetail: StateFlow<GameItem?> = _selectedGameForDetail.asStateFlow()

    private val _gameForEditing = MutableStateFlow<GameItem?>(null)
    val gameForEditing: StateFlow<GameItem?> = _gameForEditing.asStateFlow()

    private val _isFormDialogOpen = MutableStateFlow(false)
    val isFormDialogOpen: StateFlow<Boolean> = _isFormDialogOpen.asStateFlow()

    private val _messageEvent = MutableStateFlow<String?>(null)
    val messageEvent: StateFlow<String?> = _messageEvent.asStateFlow()

    private val _isPriceUpdating = MutableStateFlow(false)
    val isPriceUpdating: StateFlow<Boolean> = _isPriceUpdating.asStateFlow()

    // Multi-selection state for batch actions
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedGameIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedGameIds: StateFlow<Set<String>> = _selectedGameIds.asStateFlow()

    // Raw streams from Firestore
    private val rawCollectionFlow = repository.observeGames(isWishlist = false)
        .map<List<GameItem>, UiState<List<GameItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Échec de chargement de la collection")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val rawWishlistFlow = repository.observeGames(isWishlist = true)
        .map<List<GameItem>, UiState<List<GameItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Échec de chargement de la wishlist")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val allGamesForStats: StateFlow<List<GameItem>> = repository.observeAllGames()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Filtered & Sorted Collection Flow
    val filteredCollection: StateFlow<UiState<List<GameItem>>> = combine(
        rawCollectionFlow,
        _searchQuery,
        _selectedPlatform,
        _selectedCompleteness,
        _sortOrder
    ) { state, query, platform, completeness, sort ->
        when (state) {
            is UiState.Success -> {
                val filtered = applyFiltersAndSort(state.data, query, platform, completeness, sort)
                UiState.Success(filtered)
            }
            is UiState.Error -> state
            is UiState.Loading -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    // Filtered & Sorted Wishlist Flow (Wishlist tab has no completeness selector, so keep completeness unrestricted)
    val filteredWishlist: StateFlow<UiState<List<GameItem>>> = combine(
        rawWishlistFlow,
        _searchQuery,
        _selectedPlatform,
        _sortOrder
    ) { state, query, platform, sort ->
        when (state) {
            is UiState.Success -> {
                val filtered = applyFiltersAndSort(state.data, query, platform, "Toutes", sort)
                UiState.Success(filtered)
            }
            is UiState.Error -> state
            is UiState.Loading -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    private fun applyFiltersAndSort(
        list: List<GameItem>,
        query: String,
        platform: String,
        completeness: String,
        sort: SortOrder
    ): List<GameItem> {
        val trimmedQuery = query.trim()
        val hasQuery = trimmedQuery.isNotEmpty()
        val hasPlatformFilter = platform != "Toutes"
        val hasCompletenessFilter = completeness != "Toutes"

        val filtered = if (!hasQuery && !hasPlatformFilter && !hasCompletenessFilter) {
            list
        } else {
            list.filter { game ->
                val matchesQuery = !hasQuery ||
                    game.title.contains(trimmedQuery, ignoreCase = true) ||
                    game.platform.contains(trimmedQuery, ignoreCase = true) ||
                    game.publisher.contains(trimmedQuery, ignoreCase = true) ||
                    game.barcode.contains(trimmedQuery, ignoreCase = true)
                val matchesPlatform = !hasPlatformFilter || game.platform.equals(platform, ignoreCase = true)
                val matchesCompleteness = !hasCompletenessFilter || game.completeness.equals(completeness, ignoreCase = true)
                matchesQuery && matchesPlatform && matchesCompleteness
            }
        }

        if (filtered.size <= 1) return filtered
        return when (sort) {
            SortOrder.TITLE_ASC -> filtered.sortedWith(TITLE_ASC_COMPARATOR)
            SortOrder.VALUE_DESC -> filtered.sortedByDescending { it.estimatedValue }
            SortOrder.RATING_DESC -> filtered.sortedByDescending { it.rating }
            SortOrder.YEAR_DESC -> filtered.sortedByDescending { it.releaseYear }
            SortOrder.RECENT_ADDED -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
        if (_isSelectionMode.value) {
            setSelectionMode(false)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedPlatform(platform: String) {
        _selectedPlatform.value = platform
    }

    fun setSelectedCompleteness(completeness: String) {
        _selectedCompleteness.value = completeness
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun openGameDetail(game: GameItem) {
        _selectedGameForDetail.value = game
    }

    fun closeGameDetail() {
        _selectedGameForDetail.value = null
    }

    fun openNewGameForm(prefill: GameItem? = null) {
        _gameForEditing.value = prefill
        _isFormDialogOpen.value = true
    }

    fun openEditGameForm(game: GameItem) {
        _gameForEditing.value = game
        _isFormDialogOpen.value = true
    }

    fun closeFormDialog() {
        _isFormDialogOpen.value = false
        _gameForEditing.value = null
    }

    fun clearMessage() {
        _messageEvent.value = null
    }

    fun saveGame(game: GameItem) {
        viewModelScope.launch {
            if (game.id.isBlank()) {
                val result = repository.addGame(game)
                if (result.isSuccess) {
                    val label = if (game.isWishlist) "ajouté à votre wishlist" else "ajouté à votre collection"
                    _messageEvent.value = "${game.title} $label !"
                    closeFormDialog()
                } else {
                    _messageEvent.value = "Erreur lors de l'ajout: ${result.exceptionOrNull()?.message}"
                }
            } else {
                val result = repository.updateGame(game)
                if (result.isSuccess) {
                    _messageEvent.value = "${game.title} mis à jour avec succès."
                    closeFormDialog()
                    if (_selectedGameForDetail.value?.id == game.id) {
                        _selectedGameForDetail.value = game
                    }
                } else {
                    _messageEvent.value = "Erreur lors de la modification: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            val result = repository.deleteGame(gameId)
            if (result.isSuccess) {
                _messageEvent.value = "Jeu supprimé."
                closeGameDetail()
            } else {
                _messageEvent.value = "Erreur lors de la suppression."
            }
        }
    }

    fun toggleWishlist(game: GameItem) {
        viewModelScope.launch {
            val result = repository.toggleWishlist(game.id, game.isWishlist)
            if (result.isSuccess) {
                val newStatus = if (game.isWishlist) "déplacé dans votre collection" else "déplacé dans votre wishlist"
                _messageEvent.value = "${game.title} $newStatus !"
                closeGameDetail()
            } else {
                _messageEvent.value = "Erreur lors du déplacement."
            }
        }
    }

    fun importGames(games: List<GameItem>, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importGamesBatch(games)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: games.size
                _messageEvent.value = "$count jeu(x) importé(s) avec succès !"
            } else {
                _messageEvent.value = "Erreur lors de l'import : ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete(result)
        }
    }

    fun refreshAllGamePricesFromPriceCharting(context: Context) {
        if (_isPriceUpdating.value) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _isPriceUpdating.value = true
            _messageEvent.value = "Actualisation des cotes PriceCharting en cours..."
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val games = repository.observeGames(isWishlist = false).first()
                        var updatedCount = 0
                        var oldValueTotal = 0.0
                        var newValueTotal = 0.0

                        for (game in games) {
                            oldValueTotal += game.estimatedValue
                            val priceData = PriceChartingScraper.scrapePriceCharting(game.title, game.platform)
                            if (priceData != null) {
                                val newPrice = MonthlyPriceUpdateWorker.selectPriceForGame(game, priceData)
                                if (kotlin.math.abs(newPrice - game.estimatedValue) >= 0.10) {
                                    val updatedGame = game.withUpdatedPriceChartingValuation(newPrice)
                                    val updateRes = repository.updateGame(updatedGame)
                                    if (updateRes.isSuccess) {
                                        updatedCount++
                                        newValueTotal += newPrice
                                    } else {
                                        newValueTotal += game.estimatedValue
                                    }
                                } else {
                                    newValueTotal += game.estimatedValue
                                }
                            } else {
                                newValueTotal += game.estimatedValue
                            }
                        }

                        PriceChartingPreferenceManager.recordSuccessfulUpdate(
                            context = appContext,
                            gamesUpdated = updatedCount,
                            oldValueTotal = oldValueTotal,
                            newValueTotal = newValueTotal
                        )
                        Result.success(updatedCount to (newValueTotal - oldValueTotal))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }

                if (result.isSuccess) {
                    val (count, diff) = result.getOrThrow()
                    val diffStr = if (diff >= 0) "+${String.format(java.util.Locale.FRANCE, "%.2f", diff)} €" else "${String.format(java.util.Locale.FRANCE, "%.2f", diff)} €"
                    _messageEvent.value = "Actualisation terminée : $count jeu(x) mis à jour ($diffStr) !"
                } else {
                    _messageEvent.value = "Erreur lors de l'actualisation PriceCharting : ${result.exceptionOrNull()?.localizedMessage}"
                }
            } finally {
                _isPriceUpdating.value = false
            }
        }
    }

    fun updateSingleGamePriceFromPriceCharting(game: GameItem, onComplete: ((Result<Double>) -> Unit)? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val priceData = PriceChartingScraper.scrapePriceCharting(game.title, game.platform)
                        ?: return@withContext Result.failure(Exception("Aucune cote trouvée sur PriceCharting pour \"${game.title}\"."))

                    val newPrice = MonthlyPriceUpdateWorker.selectPriceForGame(game, priceData)
                    val updatedGame = game.withUpdatedPriceChartingValuation(newPrice)
                    val updateRes = repository.updateGame(updatedGame)
                    if (updateRes.isFailure) {
                        return@withContext Result.failure(
                            updateRes.exceptionOrNull() ?: Exception("Échec de mise à jour Firestore.")
                        )
                    }
                    Result.success(updatedGame to newPrice)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) {
                val (updatedGame, price) = result.getOrThrow()
                if (_selectedGameForDetail.value?.id == updatedGame.id) {
                    _selectedGameForDetail.value = updatedGame
                }
                _messageEvent.value = "Cote de \"${game.title}\" mise à jour : ${String.format(java.util.Locale.FRANCE, "%.2f", price)} € (PriceCharting)"
                onComplete?.invoke(Result.success(price))
            } else {
                val err = result.exceptionOrNull() ?: Exception("Erreur inconnue")
                _messageEvent.value = "Erreur PriceCharting : ${err.localizedMessage}"
                onComplete?.invoke(Result.failure(err))
            }
        }
    }

    private fun GameItem.withUpdatedPriceChartingValuation(newPrice: Double): GameItem {
        val tag = "[Cote PriceCharting: ${String.format(java.util.Locale.FRANCE, "%.2f", newPrice)} €]"
        val updatedNotes = if (PRICE_CHARTING_NOTE_REGEX.containsMatchIn(notes)) {
            notes.replace(PRICE_CHARTING_NOTE_REGEX, tag).trim()
        } else {
            val trimmed = notes.trim()
            if (trimmed.isEmpty()) {
                tag
            } else {
                val maxBaseLen = (GameItem.MAX_NOTES_LENGTH - tag.length - 1).coerceAtLeast(0)
                val base = trimmed.take(maxBaseLen).trimEnd()
                if (base.isEmpty()) tag else "$base\n$tag"
            }
        }.take(GameItem.MAX_NOTES_LENGTH)
        return copy(estimatedValue = newPrice, notes = updatedNotes)
    }

    companion object {
        private val PRICE_CHARTING_NOTE_REGEX = Regex("\\[Cote PriceCharting[^\\]]*\\]")
        private val TITLE_ASC_COMPARATOR: Comparator<GameItem> =
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }

        fun factory(repository: GameRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameDexViewModel(repository) as T
                }
            }
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedGameIds.value = emptySet()
        }
    }

    fun toggleGameSelection(gameId: String) {
        if (gameId.isBlank()) return
        var nowNonEmpty = false
        _selectedGameIds.update { current ->
            val updated = if (current.contains(gameId)) current - gameId else current + gameId
            nowNonEmpty = updated.isNotEmpty()
            updated
        }
        if (nowNonEmpty && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    fun selectAllGames(gameIds: List<String>) {
        _selectedGameIds.value = gameIds.filter { it.isNotBlank() }.toSet()
        if (_selectedGameIds.value.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedGameIds.value = emptySet()
    }

    fun batchChangePlatformForSelectedGames(newPlatform: String, onComplete: ((Result<Int>) -> Unit)? = null) {
        val ids = _selectedGameIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = repository.updatePlatformForGames(ids, newPlatform)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: ids.size
                _messageEvent.value = "$count jeu(x) déplacé(s) sur \"$newPlatform\" !"
                _selectedGameIds.value = emptySet()
                _isSelectionMode.value = false
            } else {
                _messageEvent.value = "Erreur lors du changement de console : ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete?.invoke(result)
        }
    }
}
