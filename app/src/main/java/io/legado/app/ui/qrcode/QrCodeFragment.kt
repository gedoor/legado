package io.legado.app.ui.qrcode

import com.google.zxing.Result
import com.king.zxing.CaptureFragment

class QrCodeFragment : CaptureFragment() {

    override fun onScanResultCallback(result: Result?): Boolean {
        (activity as? QrCodeActivity)?.onScanResultCallback(result)
        return true
    }

}