package org.aponcet.mygift.routes

import io.ktor.http.content.*
import io.ktor.routing.*

fun Route.static() {
    static("/") {
        resources("static")
        defaultResource("static/index.html")
    }

    static("signin") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("signup") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("mywishlist") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("myfriends") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("friend/*") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("buy-list") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("manage-account") {
        resources("static")
        defaultResource("static/index.html")
    }
    static("reset-password/*") {
        resources("static")
        defaultResource("static/index.html")
    }

    static("static") {
        resources("static/static")
    }
}