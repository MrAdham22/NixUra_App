package com.example.reclaimapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform