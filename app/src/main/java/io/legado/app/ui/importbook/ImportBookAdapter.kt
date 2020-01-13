package io.legado.app.ui.importbook

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter


class ImportBookAdapter(context: Context) :
    SimpleRecyclerAdapter<DocumentFile>(context, R.layout.item_import_book) {


    override fun convert(holder: ItemViewHolder, item: DocumentFile, payloads: MutableList<Any>) {

    }

}