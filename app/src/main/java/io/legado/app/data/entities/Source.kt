package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "sources",
    indices = [(Index(value = ["sourceId"]))])
data class Source(@PrimaryKey(autoGenerate = true)
                  @ColumnInfo(name = "sourceId")
                  var id: Int = 0,                          // 编号
                  var name: String = "",                    // 名称
                  var origin: String = "",                  // 地址，包括 http/https
                  var type: Int = 0,                        // 类型，0 文本，1 音频
                  var group: String? = null,                // 分组
                  var header: String? = null,               // header
                  var loginUrl: String? = null,             // 登录地址
                  var isEnabled: Boolean = true,            // 是否启用
                  var lastUpdateTime: Long = 0,             // 最后更新时间，用于排序
                  var customOrder: Int = 0,                 // 手动排序编号
                  var weight: Int = 0,                      // 智能排序的权重
                  var exploreRule: String? = null,          // 发现规则
                  var searchRule: String? = null,           // 搜索规则
                  var bookInfoRule: String? = null,         // 书籍信息页规则
                  var tocRule: String? = null,              // 目录页规则
                  var contentRule: String? = null           // 正文页规则
                  ) : Parcelable