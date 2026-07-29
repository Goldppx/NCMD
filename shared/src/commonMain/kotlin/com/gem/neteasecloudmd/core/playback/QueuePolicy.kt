package com.gem.neteasecloudmd.core.playback

enum class PlayMode {
    SEQUENTIAL,
    SHUFFLE,
    REPEAT_ONE
}

data class QueueState<T>(
    val items: List<T> = emptyList(),
    val currentIndex: Int = 0,
    val playMode: PlayMode = PlayMode.SEQUENTIAL
) {
    val currentItem: T?
        get() = items.getOrNull(currentIndex)
}

object QueuePolicy {
    fun <T> normalized(state: QueueState<T>): QueueState<T> {
        if (state.items.isEmpty()) return state.copy(currentIndex = 0)
        return state.copy(currentIndex = state.currentIndex.coerceIn(state.items.indices))
    }

    fun <T> nextSequential(state: QueueState<T>): Int? {
        val normalized = normalized(state)
        return normalized.currentIndex.takeIf { it < normalized.items.lastIndex }?.plus(1)
    }

    fun <T> previousSequential(state: QueueState<T>): Int? {
        val normalized = normalized(state)
        return normalized.currentIndex.takeIf { it > 0 }?.minus(1)
    }

    fun <T> afterRemoval(state: QueueState<T>, removedIndex: Int): QueueState<T> {
        if (removedIndex !in state.items.indices) return normalized(state)

        val updatedItems = state.items.toMutableList().apply { removeAt(removedIndex) }
        if (updatedItems.isEmpty()) return state.copy(items = emptyList(), currentIndex = 0)

        val newIndex = when {
            removedIndex < state.currentIndex -> state.currentIndex - 1
            state.currentIndex >= updatedItems.size -> updatedItems.lastIndex
            else -> state.currentIndex
        }
        return state.copy(items = updatedItems, currentIndex = newIndex)
    }
}
