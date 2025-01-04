package com.ksidelta.library.session

data class SessionModel(
    val associations: Map<Class<Any>, Any> = emptyMap()
)
