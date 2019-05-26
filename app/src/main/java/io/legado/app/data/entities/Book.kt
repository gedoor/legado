package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import io.legado.app.constant.AppConst.NOT_AVAILABLE
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "books", indices = [(Index(value = ["descUrl"], unique = true))])
data class Book(@PrimaryKey
                var descUrl: String = "",                   // 详情页Url(本地书源存储完整文件路径)
                var tocUrl: String = "",                    // 目录页Url (toc=table of Contents)
                var sourceId: Int = -1,                     // 书源规则id(默认-1,表示本地书籍)
                var name: String? = null,                      // 书籍名称(书源获取)
                var customName: String? = null,                // 书籍名称(用户修改)
                var author: String? = null,                 // 作者名称(书源获取)
                var customAuthor: String? = null,           // 作者名称(用户修改)
                var tag: String? = null,                    // 分类信息(书源获取)
                var customTag: String? = null,              // 分类信息(用户修改)
                var coverUrl: String? = null,               // 封面Url(书源获取)
                var customCoverUrl: String? = null,         // 封面Url(用户修改)
                var description: String? = null,            // 简介内容(书源获取)
                var customDescription: String? = null,      // 简介内容(用户修改)
                var charset: String? = null,                // 自定义字符集名称(仅适用于本地书籍)
                var type: Int = 0,                          // 0: 文本读物, 1: 有声读物
                var group: Int = 0,                         // 自定义分组索引号
                var latestChapterTitle: String? = null,     // 最新章节标题
                var latestChapterTime: Long = 0,            // 最新章节标题更新时间
                var lastCheckTime: Long = 0,                // 最近一次更新书籍信息的时间
                var lastCheckCount: Int = 0,                // 最近一次发现新章节的数量
                var totalChapterNum: Int = 0,               // 书籍目录总数
                var durChapterTitle: String? = null,           // 当前章节名称
                var durChapterIndex: Int = 0,               // 当前章节索引
                var durChapterPos: Int = 0,                 // 当前阅读的进度(首行字符的索引位置)
                var durChapterTime: Long = 0,               // 最近一次阅读书籍的时间(打开正文的时间)
                var canUpdate: Boolean = true,              // 刷新书架时更新书籍信息
                var variable: String? = null                // 自定义书籍变量信息(用于书源规则检索书籍信息)
) : Parcelable {

    fun getUnreadChapterNum() = Math.max(totalChapterNum - durChapterIndex - 1, 0)

    fun getDisplayName() = customName ?: name ?: NOT_AVAILABLE

    fun getDisplayAuthor() = customAuthor ?: author ?: NOT_AVAILABLE

    fun getDisplayCover() = customCoverUrl ?: coverUrl

    fun getDisplayDescription() = customDescription ?: description

}