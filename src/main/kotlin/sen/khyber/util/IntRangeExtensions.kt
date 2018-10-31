package sen.khyber.util

fun IntRange.exclusive() = start + 1 until endInclusive

fun IntRange.endExclusive() = start until endInclusive

fun IntRange.startExclusive() = start + 1 .. endInclusive

fun IntRange.inclusive() = start - 1 .. endInclusive + 1

fun IntRange.endInclusive() = start .. endInclusive + 1

fun IntRange.startInclusive() = start - 1 .. endInclusive