package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ReorderState(val listState: LazyListState) {
    var draggedItemIndex by mutableStateOf<Int?>(null)
    var draggedItemOffset by mutableFloatStateOf(0f)

    // Sürüklenen öğenin ekrandaki gerçek pozisyonunu hesaplar
    fun calculateCurrentOffset(index: Int): Float {
        return if (index == draggedItemIndex) draggedItemOffset else 0f
    }
}

@Composable
fun rememberReorderState(listState: LazyListState): ReorderState {
    return remember(listState) { ReorderState(listState) }
}
