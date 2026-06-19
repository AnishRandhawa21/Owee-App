package com.anish.owee.data.remote

import com.anish.owee.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseProvider {

    val client: SupabaseClient by lazy {
        val rawUrl = BuildConfig.SUPABASE_URL
        val cleanUrl = rawUrl.trim().removeSuffix("/")

        createSupabaseClient(
            supabaseUrl = cleanUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create()
            
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}