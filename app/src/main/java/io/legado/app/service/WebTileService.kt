package io.legado.app.service

import android.app.Dialog
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.WindowManager.BadTokenException
import androidx.annotation.RequiresApi
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.utils.printOnDebug


/**
 * web服务快捷开关
 */
@RequiresApi(Build.VERSION_CODES.N)
class WebTileService : TileService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                IntentAction.start -> qsTile?.run {
                    state = Tile.STATE_ACTIVE
                    updateTile()
                }

                IntentAction.stop -> qsTile?.run {
                    state = Tile.STATE_INACTIVE
                    updateTile()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val dialog = Dialog(this, R.style.AppTheme_Transparent)
                dialog.setOnShowListener {
                    try {
                        WebService.startForeground(this)
                    } catch (e: ForegroundServiceStartNotAllowedException) {
                        e.printStackTrace()
                    }
                    dialog.dismiss()
                }
                try {
                    showDialog(dialog)
                } catch (e: BadTokenException) {
                    e.printStackTrace()
                }
            } else {
                WebService.start(this)
            }
        }
    }

}