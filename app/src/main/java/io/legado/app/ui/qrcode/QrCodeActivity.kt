package io.legado.app.ui.qrcode

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import cn.bingoogolapple.qrcode.core.QRCodeView
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.utils.readBytes
import kotlinx.android.synthetic.main.activity_qrcode_capture.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.toast

class QrCodeActivity : BaseActivity(R.layout.activity_qrcode_capture), QRCodeView.Delegate {

    private val requestQrImage = 202
    private var flashlightIsOpen: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        zxingview.setDelegate(this)
        fab_flashlight.setOnClickListener {
            if (flashlightIsOpen) {
                flashlightIsOpen = false
                zxingview.closeFlashlight()
            } else {
                flashlightIsOpen = true
                zxingview.openFlashlight()
            }
        }
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

    override fun onStart() {
        super.onStart()
        startCamera()
    }

    private fun startCamera() {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.CAMERA)
            .rationale(R.string.qr_per)
            .onGranted {
                zxingview.visibility = View.VISIBLE
                zxingview.startSpotAndShowRect() // 显示扫描框，并开始识别
            }.request()
    }

    override fun onStop() {
        zxingview.stopCamera() // 关闭摄像头预览，并且隐藏扫描框
        super.onStop()
    }

    override fun onDestroy() {
        zxingview.onDestroy() // 销毁二维码扫描控件
        super.onDestroy()
    }

    override fun onScanQRCodeSuccess(result: String) {
        val intent = Intent()
        intent.putExtra("result", result)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {

    }

    override fun onScanQRCodeOpenCameraError() {
        toast("打开相机失败")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.data?.let {
            zxingview.startSpotAndShowRect() // 显示扫描框，并开始识别

            if (resultCode == Activity.RESULT_OK && requestCode == requestQrImage) {
                // 本来就用到 QRCodeView 时可直接调 QRCodeView 的方法，走通用的回调
                it.readBytes(this)?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    zxingview.decodeQRCode(bitmap)
                }
            }
        }
    }

}