package devlight.io.xtreeivi.parallaximageview.utils

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import devlight.io.xtreeivi.parallaximageview.ParallaxImageView

/**
 * This util class designed specially to use with [RecyclerView],
 * when its items uses [ParallaxImageView] to simplify update of parallax effect
 */
class ParallaxScrollListener : RecyclerView.OnScrollListener() {

    private val viewTypePivMap = mutableMapOf<Int, MutableSet<Int>>()
    private val bannedIdsSet = mutableSetOf<Int>()

    private fun updateScroll(recyclerView: RecyclerView) {
        var found = false

        fun searchPiv(view: View, id: Int) {
            if (view is ParallaxImageView) {
                if (!found && !viewTypePivMap.containsKey(id)) {
                    found = true
                    viewTypePivMap[id] = mutableSetOf()
                }
                viewTypePivMap[id]?.add(view.id)
                view.updateParallax(recyclerView)
            } else if (view is ViewGroup) for (j in 0 until view.childCount) searchPiv(view.getChildAt(j)
                    ?: continue, id)
        }

        (0 until recyclerView.childCount)
                .asSequence()
                .mapNotNull { recyclerView.getChildAt(it) }
                .filterNot { bannedIdsSet.contains(it.id) }
                .forEach { item ->
                    if (item is ParallaxImageView) item.updateParallax(recyclerView)
                    else {
                        if (viewTypePivMap.containsKey(item.id)) {
                            viewTypePivMap[item.id]?.forEach { item.findViewById<ParallaxImageView>(it)?.updateParallax(recyclerView) }
                        } else if (item is ViewGroup) {
                            // inner function
                            found = false
                            searchPiv(item, item.id)
                            if (!found) bannedIdsSet.add(item.id)
                        } else bannedIdsSet.add(item.id)
                    }
                }
    }

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        updateScroll(recyclerView ?: return)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
        updateScroll(recyclerView ?: return)
    }
}