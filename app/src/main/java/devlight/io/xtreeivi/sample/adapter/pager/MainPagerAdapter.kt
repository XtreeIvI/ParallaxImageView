package devlight.io.xtreeivi.sample.adapter.pager

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.PagerAdapter
import android.view.ViewGroup

import devlight.io.xtreeivi.sample.R
import devlight.io.xtreeivi.sample.fragment.ParallaxListFragment
import devlight.io.xtreeivi.sample.fragment.WidgetFragment

class MainPagerAdapter(context: Context?, fm: FragmentManager) : SmartFragmentStatePagerAdapter<Fragment>(fm) {

    private val pageCount = 2
    private val titles = arrayOfNulls<String>(2)

    init {
        titles[0] = context!!.resources.getString(R.string.piv_widget)
        titles[1] = context.resources.getString(R.string.piv_list)
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (!isInRange(position)) "" else titles[position]
    }

    private fun isInRange(position: Int): Boolean {
        return position >= 0 && position <= pageCount - 1
    }


    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
    }

    override fun getItem(index: Int): Fragment? {
        when (index) {
            0 -> return WidgetFragment()
            1 -> return ParallaxListFragment()
        }
        return null
    }

    override fun getCount(): Int {
        return pageCount
    }
}