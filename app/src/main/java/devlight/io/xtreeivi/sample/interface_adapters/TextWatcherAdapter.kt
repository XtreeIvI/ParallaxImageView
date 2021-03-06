package devlight.io.xtreeivi.sample.interface_adapters

import android.text.Editable
import android.text.TextWatcher

/**
 * Created by XtreeIvI on 18.03.2018.
 */
interface TextWatcherAdapter : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?)
}
