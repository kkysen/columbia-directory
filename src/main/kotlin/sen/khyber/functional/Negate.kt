package sen.khyber.functional

fun <T> ((T) -> Boolean).negate(): (T) -> Boolean = { t -> !this(t) }

operator fun <T> ((T) -> Boolean).not(): (T) -> Boolean = negate()

fun <T, U> ((T, U) -> Boolean).negate(): (T, U) -> Boolean = { t, u -> !this(t, u) }

operator fun <T, U> ((T, U) -> Boolean).not(): (T, U) -> Boolean = negate()

fun <T, U, V> ((T, U, V) -> Boolean).negate(): (T, U, V) -> Boolean = { t, u, v -> !this(t, u, v) }

operator fun <T, U, V> ((T, U, V) -> Boolean).not(): (T, U, V) -> Boolean = negate()