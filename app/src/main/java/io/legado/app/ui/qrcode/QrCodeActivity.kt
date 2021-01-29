package io.legado.app.ui.qrcode

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.Result
import com.king.zxing.CameraScan.OnScanResultCallback
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityQrcodeCaptureBinding
import io.legado.app.utils.QRCodeUtils
import io.legado.app.utils.readBytes

class QrCodeActivity : BaseActivity<ActivityQrcodeCaptureBinding>(), OnScanResultCallback {

    private val requestQrImage = 202

    override fun getViewBinding(): ActivityQrcodeCaptureBinding {
        return ActivityQrcodeCaptureBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "qrCodeFragment"
        var qrCodeFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (qrCodeFragment == null) qrCodeFragment = QrCodeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_content, qrCodeFragment, fTag)
            .commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.qr_code_scan, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_choose_from_gallery -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, requestQrImage)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onScanResultCallback(result: Result?): Boolean {
        val intent = Intent()
        intent.putExtra("result", result?.text)
        setResult(RESULT_OK, intent)
        finish()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.data?.let {
            if (resultCode == Activity.RESULT_OK && requestCode == requestQrImage) {
                it.readBytes(this)?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    onScanResultCallback(QRCodeUtils.parseCodeResult(bitmap))
                }
            }
        }
    }

}