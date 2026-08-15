package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CommandHistory
import com.example.data.CommandHistoryDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommandViewModel(private val dao: CommandHistoryDao) : ViewModel() {
    val history: StateFlow<List<CommandHistory>> = dao.getAllCommands()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCommand(commandText: String) {
        viewModelScope.launch {
            dao.insertCommand(CommandHistory(command = commandText))
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }
}

class CommandViewModelFactory(private val dao: CommandHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommandViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommandViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
