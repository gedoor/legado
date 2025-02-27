package io.legado.app.model.recyclerView


data class MangaContent(
    var mChapterPagePos: Int = 0,//总章节位置
    var mChapterPageCount: Int,//总章节数量
    var mChapterNextPagePos: Int = 0,//下一章
    val mImageUrl: String = "",//当前URL
    var mDurChapterPos: Int = 0,//当前章节位置
    var mDurChapterCount: Int = 0,//当前章节内容总数
    var mChapterName:String="",//章节名称
)