package com.gem.neteasecloudmd.api

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private lateinit var playerManager: PlayerManager

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerManager.getInstance(applicationContext)
        val session = playerManager.mediaSessionForService()
        if (!isSessionAdded(session)) {
            addSession(session)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        playerManager.mediaSessionForService()

    override fun onDestroy() {
        playerManager.releaseServiceResources()
        super.onDestroy()
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, PlaybackService::class.java)
    }
}
