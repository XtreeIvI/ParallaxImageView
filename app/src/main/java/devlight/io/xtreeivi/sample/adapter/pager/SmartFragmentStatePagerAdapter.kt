package devlight.io.xtreeivi.sample.adapter.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import android.view.ViewGroup

abstract class SmartFragmentStatePagerAdapter<T : Fragment>(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager) {

    val registeredFragments = SparseArray<T>()
    var position: Int = 0
        private set

    // Register the fragment when the item is instantiated
    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val fragment = super.instantiateItem(container, position) as T
        registeredFragments.put(position, fragment)
        return fragment
    }

    // Unregister when the item is inactive
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    // Returns the fragment for the position (if instantiated)
    fun getRegisteredFragment(position: Int): T {
        return registeredFragments.get(position)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        this.position = position
    }
}