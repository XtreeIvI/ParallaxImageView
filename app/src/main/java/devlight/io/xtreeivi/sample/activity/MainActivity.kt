package devlight.io.xtreeivi.sample.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import devlight.io.xtreeivi.sample.R
import devlight.io.xtreeivi.sample.adapter.pager.MainPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUi()
    }

    private fun setUi() {
        vpMain.offscreenPageLimit = 1
        vpMain.adapter = MainPagerAdapter(this, supportFragmentManager)
        tlMain.setupWithViewPager(vpMain)
    }
}

