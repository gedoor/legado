package io.legado.app.utils.canvasrecorder.pools

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import io.legado.app.utils.canvasrecorder.objectpool.BaseObjectPool

@RequiresApi(Build.VERSION_CODES.Q)
class RenderNodePool : BaseObjectPool<RenderNode>() {

    override fun recycle(target: RenderNode) {
        target.discardDisplayList()
        super.recycle(target)
    }

    override fun create(): RenderNode = RenderNode("CanvasRecorder")

}
