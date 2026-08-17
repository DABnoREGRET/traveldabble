package com.dabber.traveldabble

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform