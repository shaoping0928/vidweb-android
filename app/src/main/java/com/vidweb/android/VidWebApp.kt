package com.vidweb.android

import android.app.Application
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.Prefs
import com.vidweb.android.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VidWebApp : Application() {

    lateinit var repo: Repo
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repo = Repo(this, appScope)
        appScope.launch {
            val url = runCatching { repo.prefs.serverUrl.first() }.getOrDefault(Prefs.DEFAULT_SERVER)
            ApiClient.setBaseUrl(url)
            repo.loadState()
        }
    }

    companion object {
        fun repo(app: Application): Repo = (app as VidWebApp).repo
        fun scope(app: Application): CoroutineScope = (app as VidWebApp).appScope
    }
}
