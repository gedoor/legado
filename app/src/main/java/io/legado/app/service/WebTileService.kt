package io.legado.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import io.legado.app.constant.IntentAction
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.startService

/**
 * web服务快捷开关
 */
@RequiresApi(Build.VERSION_CODES.N)
class WebTileService : TileService() {

    companion object {

        fun setState(context: Context, active: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.startService<WebTileService> {
                    action = if (active) {
                        IntentAction.start
                    } else {
                        IntentAction.stop
                    }
                }
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                IntentAction.start -> {
                    qsTile.state = Tile.STATE_ACTIVE
                    qsTile.updateTile()
                }
                IntentAction.stop -> {
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onStartListening() {
        super.onStartListening()
        if (WebService.isRun) {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()
        } else {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (WebService.isRun) {
            WebService.stop(this)
        } else {
            WebService.start(this)
        }
    }

}