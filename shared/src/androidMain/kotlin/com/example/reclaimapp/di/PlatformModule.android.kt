package com.example.reclaimapp.di

import android.content.Context
import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.local.getDatabaseBuilder
import com.example.reclaimapp.data.local.getRoomDatabase
import org.koin.dsl.module

fun platformModule(context: Context) = module {
    single<NixUraDatabase> {
        getRoomDatabase(getDatabaseBuilder(context))
    }
}
