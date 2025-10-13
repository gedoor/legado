package io.legado.app.ui.video

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookChapter

class ChapterAdapter(
    private val chapters: List<BookChapter>,
    private val onChapterClick: (BookChapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(chapters[position])
    }

    override fun getItemCount(): Int = chapters.size

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChapterName: TextView = itemView.findViewById(R.id.tvChapterName)

        fun bind(chapter: BookChapter) {
            tvChapterName.text = chapter.title
            tvChapterName.setBackgroundResource(R.drawable.bg_video_chapter_item)
            tvChapterName.setTextColor(Color.WHITE)
            itemView.setOnClickListener {
                onChapterClick(chapter)
            }
        }
    }
}