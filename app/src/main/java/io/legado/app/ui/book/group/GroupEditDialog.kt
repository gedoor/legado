package io.legado.app.ui.book.group

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogBookGroupEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick
import java.io.FileOutputStream

class GroupEditDialog() : BaseDialogFragment(R.layout.dialog_book_group_edit) {

    constructor(bookGroup: BookGroup? = null) : this() {
        arguments = Bundle().apply {
            putParcelable("group", bookGroup)
        }
    }

    private val binding by viewBinding(DialogBookGroupEditBinding::bind)
    private val viewModel by viewModels<GroupViewModel>()
    private var bookGroup: BookGroup? = null
    val selectImage = registerForActivityResult(SelectImageContract()) {
        readUri(it?.uri) { fileDoc, inputStream ->
            var file = requireContext().externalFiles
            file = FileUtils.createFileIfNotExist(file, "covers", fileDoc.name)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            binding.ivCover.load(file.absolutePath)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        @Suppress("DEPRECATION")
        bookGroup = arguments?.getParcelable("group")
        bookGroup?.let {
            binding.tieGroupName.setText(it.groupName)
            binding.ivCover.load(it.cover)
        } ?: let {
            binding.toolBar.title = getString(R.string.add_group)
            binding.btnDelete.gone()
            binding.ivCover.load()
        }
        binding.run {
            ivCover.onClick {
                selectImage.launch()
            }
            btnCancel.onClick {
                dismiss()
            }
            btnOk.onClick {
                val groupName = tieGroupName.text?.toString()
                if (groupName.isNullOrEmpty()) {
                    toastOnUi("分组名称不能为空")
                } else {
                    bookGroup?.let {
                        it.groupName = groupName
                        it.cover = binding.ivCover.bitmapPath
                        viewModel.upGroup(it) {
                            dismiss()
                        }
                    } ?: let {
                        viewModel.addGroup(groupName, binding.ivCover.bitmapPath) {
                            dismiss()
                        }
                    }
                }

            }
            btnDelete.onClick {
                deleteGroup {
                    bookGroup?.let {
                        viewModel.delGroup(it) {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun deleteGroup(ok: () -> Unit) {
        alert(R.string.delete, R.string.sure_del) {
            yesButton {
                ok.invoke()
            }
            noButton()
        }
    }

}