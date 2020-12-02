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
import io.legado.app.databinding.ActivityQrcodeCaptureBinding
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.utils.readBytes
import org.jetbrains.anko.toast

class QrCodeActivity : BaseActivity<ActivityQrcodeCaptureBinding>(), QRCodeView.Delegate {

    private val requestQrImage = 202
    private var flashlightIsOpen: Boolean = false

    override fun getViewBinding(): ActivityQrcodeCaptureBinding {
        return ActivityQrcodeCaptureBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.zXingView.setDelegate(this)
        binding.fabFlashlight.setOnClickListener {
            if (flashlightIsOpen) {
                flashlightIsOpen = false
                binding.zXingView.closeFlashlight()
            } else {
                flashlightIsOpen = true
                binding.zXingView.openFlashlight()
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
                binding.zXingView.visibility = View.VISIBLE
                //TODO 显示扫描框，并开始识别
                binding.zXingView.startSpotAndShowRect()
            }.request()
    }

    override fun onStop() {
        //TODO 关闭摄像头预览，并且隐藏扫描框
        binding.zXingView.stopCamera()
        super.onStop()
    }

    override fun onDestroy() {
        //TODO 销毁二维码扫描控件
        binding.zXingView.onDestroy()
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
            //TODO 显示扫描框，并开始识别
            binding.zXingView.startSpotAndShowRect()

            if (resultCode == Activity.RESULT_OK && requestCode == requestQrImage) {
                // 本来就用到 QRCodeView 时可直接调 QRCodeView 的方法，走通用的回调
                it.readBytes(this)?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.zXingView.decodeQRCode(bitmap)
                }
            }
        }
    }

}