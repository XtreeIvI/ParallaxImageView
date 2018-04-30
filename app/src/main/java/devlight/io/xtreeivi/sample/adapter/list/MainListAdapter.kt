package devlight.io.xtreeivi.sample.adapter.list

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView
import devlight.io.xtreeivi.parallaximageview.utils.FitCenterNoClipTransformation
import devlight.io.xtreeivi.sample.R
import devlight.io.xtreeivi.sample.model.ListItemModel

/**
 * Created by XtreeIvI on 09.04.2018.
 */
class MainListAdapter(var context: Context): BaseMultiItemQuickAdapter<ListItemModel, BaseViewHolder>(null) {

    private val fitCenterNoClipTransformation = FitCenterNoClipTransformation()


    init {
        addItemType(0, R.layout.item_list_example_type_0)

        val data = mutableListOf<ListItemModel>()
        data.addAll(Array(10, { ListItemModel(0, listOf(R.drawable.square_image_1)) }).toMutableList())
        setNewData(data)
    }

    override fun convert(helper: BaseViewHolder, item: ListItemModel) {
        when(item.itemType) {
            0 -> {
                val image = helper.getView<ParallaxImageView>(R.id.parallaxImageViewItem)
                Glide.with(context).load(item.images[0])
                        .apply(RequestOptions.bitmapTransform(fitCenterNoClipTransformation))
                        .into(image)
            }
        }
    }
}