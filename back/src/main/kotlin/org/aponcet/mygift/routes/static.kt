package org.aponcet.mygift.routes

import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Route.static() {
    staticResources("/", "static", "index.html")
    staticResources("/signin", "static", "index.html")
    staticResources("/signup", "static", "index.html")
    staticResources("/mywishlist", "static", "index.html")
    staticResources("/myfriends", "static", "index.html")
    staticResources("/friend/*", "static", "index.html")
    staticResources("/buylist", "static", "index.html")
    staticResources("/manageaccount", "static", "index.html")
    staticResources("/reset-password/*", "static", "index.html")
    staticResources("/changeaccount", "static", "index.html")
}