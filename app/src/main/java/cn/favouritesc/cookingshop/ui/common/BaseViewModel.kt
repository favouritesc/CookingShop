package cn.favouritesc.cookingshop.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    protected fun clearError() {
        _error.value = null
    }

    protected fun launchCoroutine(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()
                block()
            } catch (e: Exception) {
                setError(e.message ?: "发生未知错误")
            } finally {
                setLoading(false)
            }
        }
    }

    protected fun launchCollection(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}
