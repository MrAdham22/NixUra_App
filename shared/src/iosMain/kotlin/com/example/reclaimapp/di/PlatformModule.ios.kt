package com.example.reclaimapp.di

import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.local.getDatabaseBuilder
import com.example.reclaimapp.data.local.getRoomDatabase
import org.koin.dsl.module

fun platformModule() = module {
    single<NixUraDatabase> {
        getRoomDatabase(getDatabaseBuilder())
    }
}
