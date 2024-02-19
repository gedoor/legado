package io.legado.app.utils.canvasrecorder.pools

import android.graphics.Picture
import io.legado.app.utils.canvasrecorder.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>() {

    override fun create(): Picture = Picture()

}
