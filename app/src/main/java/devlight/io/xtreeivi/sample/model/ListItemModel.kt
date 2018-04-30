package devlight.io.xtreeivi.sample.model

import com.chad.library.adapter.base.entity.MultiItemEntity

/**
 * Created by XtreeIvI on 09.04.2018.
 */
data class ListItemModel(
        private val type:Int = 0,
        val images: List<Any> = mutableListOf(),
        val scaleTypes: List<Int> = mutableListOf()
) : MultiItemEntity {
    override fun getItemType() = type

}