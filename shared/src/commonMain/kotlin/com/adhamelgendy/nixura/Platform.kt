package com.adhamelgendy.nixura

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform