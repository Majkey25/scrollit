package cz.teply.scrollit

sealed interface AutoScrollResult {
    data object Started : AutoScrollResult

    data class Failed(val message: String) : AutoScrollResult
}
