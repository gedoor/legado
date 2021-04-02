package io.legado.app.ui.book.toc

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class TocActivityResult : ActivityResultContract<String, Pair<Int, Int>?>() {

    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, ChapterListActivity::class.java)
            .putExtra("bookUrl", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, Int>? {
        if (resultCode == RESULT_OK) {
            intent?.let {
                return Pair(
                    it.getIntExtra("index", 0),
                    it.getIntExtra("chapterPos", 0)
                )
            }
        }
        return null
    }
}