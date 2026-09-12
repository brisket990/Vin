package com.xothiques.vin.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.WishlistItemDto
import com.xothiques.vin.data.repository.WishlistRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<WishlistItemDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<WishlistItemDto>>> = _listState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Unit>?>(null)
    val submitState: StateFlow<UiState<Unit>?> = _submitState.asStateFlow()

    private val _convertState = MutableStateFlow<UiState<Unit>?>(null)
    val convertState: StateFlow<UiState<Unit>?> = _convertState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _listState.value = UiState.Loading
            _listState.value = try {
                UiState.Success(wishlistRepository.findAll())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun create(name: String, region: String?, notes: String?, targetPriceCents: Int?) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                wishlistRepository.create(name, region, notes, targetPriceCents)
                load()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try {
                wishlistRepository.remove(id)
                load()
            } catch (t: Throwable) {
                _listState.value = UiState.Error(t.toUserMessage())
            }
        }
    }

    fun convertToBottle(id: String, color: String) {
        viewModelScope.launch {
            _convertState.value = UiState.Loading
            _convertState.value = try {
                wishlistRepository.convertToBottle(id, color, locationId = null)
                load()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = null
    }

    fun resetConvertState() {
        _convertState.value = null
    }
}
