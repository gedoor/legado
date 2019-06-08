package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(
    tableName = "explore_search_urls",
    indices = [(Index(value = ["sourceId", "url"], unique = true))],
    foreignKeys = [(ForeignKey(
        entity = BookSource::class,
        parentColumns = ["sourceId"],
        childColumns = ["sourceId"],
        onDelete = ForeignKey.CASCADE
    ))]
)       // 删除书源时自动删除章节
data class ExploreSearchUrl(
    @PrimaryKey(autoGenerate = true)
    var esId: Int = 0,                          // 编号
    var sourceId: Int = 0,                      // 书源Id
    var name: String = "",                      // 发现名称，搜索可以没有
    var url: String = "",                       // 地址
    var type: Int = 0,                          // 类型，0 为发现，1 为搜索
    var isEnabled: Boolean = true,              // 是否启用
    var defOrder: Int = 0,                      // 默认排序，是在编辑书源的时候的顺序
    var usage: Int = 0,                         // 使用次数，用于按使用次数排序
    var lastUseTime: Long = 0L                  // 最后一次使用的时间
) : Parcelable

