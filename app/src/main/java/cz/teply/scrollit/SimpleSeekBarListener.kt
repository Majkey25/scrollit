package cz.teply.scrollit

import android.widget.SeekBar

class SimpleSeekBarListener(
    private val onValueChanged: () -> Unit,
) : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        onValueChanged()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
}
