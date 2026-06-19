package com.anish.owee.data.remote

import android.util.Log
import com.anish.owee.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

object SupabaseProvider {

    val client: SupabaseClient by lazy {
        val rawUrl = BuildConfig.SUPABASE_URL
        val cleanUrl = rawUrl.trim().removeSuffix("/")
        Log.d("OWEE_CONFIG", "Initializing Supabase with URL: '$cleanUrl'")

        createSupabaseClient(
            supabaseUrl = cleanUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create()
            
            install(Auth)
            install(Postgrest)
            install(Realtime)

            // Using the recommended way to configure the internal Ktor client in recent Supabase-kt
            @OptIn(SupabaseInternal::class)
            httpConfig {
                install(Logging) {
                    level = LogLevel.INFO
                    logger = object : Logger {
                        override fun log(message: String) {
                            Log.d("OWEE_KTOR", message)
                        }
                    }
                }
            }
        }
    }
}