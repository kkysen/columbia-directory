package sen.khyber.io.web

import org.openqa.selenium.support.ui.FluentWait

fun <T> FluentWait<T>.untilTrue(predicate: (T) -> Boolean): Unit {
    until(predicate)
}