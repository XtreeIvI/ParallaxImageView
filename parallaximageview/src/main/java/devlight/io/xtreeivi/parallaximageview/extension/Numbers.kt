package devlight.io.xtreeivi.parallaximageview.extension

infix fun Int.containsFlag(flag: Int) = this or flag == this

infix fun Int.addFlag(flag: Int) = this.or(flag)

infix fun Int.toggleFlag(flag: Int) = this.xor(flag)

infix fun Int.removeFlag(flag: Int) = this.and(flag.inv())

infix fun <T : Comparable<T>> T.clamp(range: ClosedRange<T>): T {
    return when {
        this in range -> this
        this < range.start -> range.start
        else -> range.endInclusive
    }
}

fun <T : Number> T.clampFraction(): T {
    val doubleValue = this.toDouble()
    return when {
        doubleValue in 0.0..1.0 -> this
        doubleValue < 0.0 -> 0.0 as T
        else -> 1.0 as T
    }
}
