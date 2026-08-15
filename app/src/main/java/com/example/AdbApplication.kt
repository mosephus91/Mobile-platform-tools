package com.example

import android.app.Application
import com.example.data.AppDatabase

class AdbApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
