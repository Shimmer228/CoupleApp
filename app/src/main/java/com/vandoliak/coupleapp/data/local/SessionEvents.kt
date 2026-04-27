package com.vandoliak.coupleapp.data.local

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SessionDestination {
    LOGIN,
    PAIR
}

object SessionEvents {
    private val _events = MutableSharedFlow<SessionDestination>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events = _events.asSharedFlow()

    fun emit(destination: SessionDestination) {
        _events.tryEmit(destination)
    }
}
