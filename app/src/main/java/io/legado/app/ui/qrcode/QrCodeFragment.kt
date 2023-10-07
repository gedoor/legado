package io.legado.app.ui.qrcode

import com.google.zxing.Result
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.zxing.BarcodeCameraScanFragment
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.MultiFormatAnalyzer
import com.king.zxing.analyze.QRCodeAnalyzer

class QrCodeFragment : BarcodeCameraScanFragment() {

    override fun initCameraScan(cameraScan: CameraScan<Result>) {
        super.initCameraScan(cameraScan)
        //初始化解码配置
        val decodeConfig = DecodeConfig()
        //如果只有识别二维码的需求，这样设置效率会更高，不设置默认为DecodeFormatManager.DEFAULT_HINTS
        decodeConfig.hints = DecodeFormatManager.QR_CODE_HINTS
        //设置是否全区域识别，默认false
        decodeConfig.isFullAreaScan = true
        //设置识别区域比例，默认0.8，设置的比例最终会在预览区域裁剪基于此比例的一个矩形进行扫码识别
        decodeConfig.areaRectRatio = 0.8f

        //在启动预览之前，设置分析器，只识别二维码
        cameraScan.setAnalyzer(MultiFormatAnalyzer(decodeConfig))
    }

    override fun createAnalyzer(): Analyzer<Result> {
        // 初始化解码配置
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS) //如果只有识别二维码的需求，这样设置效率会更高，不设置默认为DecodeFormatManager.DEFAULT_HINTS
            .setFullAreaScan(false) //设置是否全区域识别，默认false
            .setAreaRectRatio(0.8f) //设置识别区域比例，默认0.8，设置的比例最终会在预览区域裁剪基于此比例的一个矩形进行扫码识别
            .setAreaRectVerticalOffset(0).areaRectHorizontalOffset =
            0 //设置识别区域水平方向偏移量，默认为0，为0表示居中，可以为负数

        // BarcodeCameraScanActivity默认使用的MultiFormatAnalyzer，这里可以改为使用QRCodeAnalyzer
        return QRCodeAnalyzer(decodeConfig)
    }

    override fun onScanResultCallback(result: AnalyzeResult<Result>) {
        cameraScan.setAnalyzeImage(false)
        (activity as? QrCodeActivity)?.onScanResultCallback(result.result)
    }

}