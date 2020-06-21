package io.hugogu.matchito

data class AssociationResult<T, K>(
    val leftOnly: Collection<T>,
    val rightOnly: Collection<T>,
    val matches: Map<K, ValuePair<T>>
) {
    fun thenMatchProperties(vararg getProperties: (T) -> Pair<String, Comparable<*>>) = thenMatchBy {
        getProperties.map { getProperty -> getProperty(it) }.toMap()
    }

    fun thenMatchBy(getProperties: (T) -> Map<String, Comparable<*>>) =
        ListPropertyMatchingResult(leftOnly, rightOnly, matches.values.map { it.compareWith(getProperties) })
}

data class PropertyMatchingResult<out T>(
    val left: T,
    val right: T,
    val unequalProperties: Map<String, ValuePair<Any>>
) {
    fun fullyMatch() = unequalProperties.isEmpty()
}

data class ValuePair<out T>(
    val leftValue: T?,
    val rightValue: T?
) {
    fun isDifferent() = leftValue != rightValue

    fun compareWith(getProperties: (T) -> Map<String, Comparable<*>>) = compare(leftValue!!, rightValue!!, getProperties)
}

data class ListPropertyMatchingResult<T>(
    val leftOnly: Collection<T>,
    val rightOnly: Collection<T>,
    val matches: List<PropertyMatchingResult<T>>
) {
    val fullMatches by lazy(LazyThreadSafetyMode.NONE) {
        matches.filter { it.fullyMatch() }
    }

    val partialMatches by lazy(LazyThreadSafetyMode.NONE) {
        matches.filter { !it.fullyMatch() }
    }
}

data class GroupingResult<K, V>(
    val leftOnly: Map<K, List<V>>,
    val rightOnly: Map<K, List<V>>,
    val matches: Map<K, ValuePair<List<V>>>
)

data class ComparingList<T>(
    val left: Iterable<T>,
    val right: Iterable<T>
) {
    fun <K> associateBy(getKey: (T) -> K): AssociationResult<T, K> {
        val leftMap = left.associateByTo(mutableMapOf(), getKey)
        val rightMap = right.associateByTo(mutableMapOf(), getKey)
        val commonValues = extractCommon(leftMap, rightMap)

        return AssociationResult(leftMap.values, rightMap.values, commonValues)
    }

    fun <K> groupBy(getKey: (T) -> K): GroupingResult<K, T> {
        val leftMap = left.groupByTo(mutableMapOf(), getKey)
        val rightMap = right.groupByTo(mutableMapOf(), getKey)
        val commonValues = extractCommon(leftMap, rightMap)

        return GroupingResult(leftMap, rightMap, commonValues)
    }

    fun matchSingleBy(comparator: Comparator<T>): Iterable<ValuePair<T>> {
        return left.mapNotNull { l ->
            right.singleOrNull { r ->
                // If there are more than one matches, leave them as it is, not taken as pair.
                comparator.areEqual(l, r) && left.count { comparator.areEqual(it, r) } == 1
            }?.let { r -> ValuePair(l, r) }
        }
    }
}

fun <T> Comparator<T>.areEqual(l: T, r: T) = compare(l, r) == 0

fun <K, V> extractCommon(leftMap: MutableMap<K, V>, rightMap: MutableMap<K, V>) =
    (leftMap.keys intersect rightMap.keys).map {
        it to ValuePair<V>(leftMap.remove(it), rightMap.remove(it))
    }.toMap()

fun <T> compare(left: T, right: T, getProperties: (T) -> Map<String, Comparable<*>>) = run {
    val leftProperties = getProperties(left)
    val rightProperties = getProperties(right)
    check(leftProperties.size == rightProperties.size)
    check(leftProperties.keys == rightProperties.keys)
    val unequalProperties = leftProperties.keys
        .map { it to ValuePair<Any>(leftProperties[it], rightProperties[it]) }
        .filter { it.second.isDifferent() }
        .toMap()
    PropertyMatchingResult(left, right, unequalProperties)
}

fun <T> compare(left: Iterable<T>, right: Iterable<T>) = ComparingList(left, right)
