package cz.teply.scrollit

enum class ScrollMode(
    val storedValue: String,
) {
    TOUCH("touch"),
    AUTO_SCROLL("auto_scroll"),
    ;

    companion object {
        fun fromStoredValue(value: String?): ScrollMode =
            entries.firstOrNull { it.storedValue == value } ?: TOUCH
    }
}
