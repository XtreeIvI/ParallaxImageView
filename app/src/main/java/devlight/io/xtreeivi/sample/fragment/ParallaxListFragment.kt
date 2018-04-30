package devlight.io.xtreeivi.sample.fragment

import devlight.io.xtreeivi.parallaximageview.utils.ParallaxScrollListener
import devlight.io.xtreeivi.sample.R
import devlight.io.xtreeivi.sample.adapter.list.MainListAdapter
import devlight.io.xtreeivi.sample.fragment.core.BaseFragment
import kotlinx.android.synthetic.main.fragment_list.*

/**
 * Created by GIGAMOLE on 8/23/16.
 */
class ParallaxListFragment : BaseFragment() {

    override fun containerView(): Int = R.layout.fragment_list

    override fun setUI() {
        rvExampleList.setHasFixedSize(true)
        rvExampleList.adapter = MainListAdapter(this@ParallaxListFragment.activity!!)
        rvExampleList.addOnScrollListener(ParallaxScrollListener())
    }
}
