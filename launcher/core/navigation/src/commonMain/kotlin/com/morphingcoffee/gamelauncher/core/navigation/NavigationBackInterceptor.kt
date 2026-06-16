package com.morphingcoffee.gamelauncher.core.navigation

/**
 * Optional screen-level back handler. Return `true` when the event is consumed
 * (e.g. modal dismissed) and navigation should not pop the back stack.
 */
object NavigationBackInterceptor {
    var handler: (() -> Boolean)? = null
}
