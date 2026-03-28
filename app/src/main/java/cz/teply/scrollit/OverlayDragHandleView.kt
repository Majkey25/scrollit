package cz.teply.scrollit

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class OverlayDragHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean {
        return super.performClick()
    }
}
