package com.ksidelta.library.session

import com.ksidelta.library.store.Store
import io.ktor.server.routing.RoutingCall
import java.util.UUID

class KtorSessionRepository(val storage: Store) {
    fun fetch(call: RoutingCall): Session {
        val sessionId = call.request.cookies["SESSION"]
            ?: UUID.randomUUID().toString().also { id ->
                call.response.cookies.append("SESSION", id, path = "/")
            }

        return StorageSession(storage, UUID.fromString(sessionId))
    }

}