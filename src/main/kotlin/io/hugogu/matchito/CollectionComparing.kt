package io.hugogu.matchito

data class AssociationResult<T>(
    val leftOnly: Collection<T>,
    val rightOnly: Collection<T>,
    val matches: List<Pair<T, T>>
) {
    fun thenMatchProperties(vararg getProperties: (T) -> Pair<String, Comparable<*>>) = thenMatchBy {
        getProperties.map { getProperty -> getProperty(it) }.toMap()
    }

    fun thenMatchBy(getProperties: (T) -> Map<String, Comparable<*>>) =
        ListPropertyMatchingResult(leftOnly, rightOnly, matches.map { it.compareWith(getProperties) })
}

data class PropertyMatchingResult<T>(
    val left: T,
    val right: T,
    val unequalProperties: Set<String>
) {
    fun fullyMatch() = unequalProperties.isEmpty()
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
    val matches: Map<K, Pair<List<V>, List<V>>>
)

data class ComparingList<T>(
    val left: Iterable<T>,
    val right: Iterable<T>
) {
    fun <K> associateBy(getKey: (T) -> K): AssociationResult<T> {
        val leftMap = left.associateByTo(mutableMapOf(), getKey)
        val rightMap = right.associateByTo(mutableMapOf(), getKey)
        val commonKeys = leftMap.keys intersect rightMap.keys
        val commonValues = commonKeys.map {
            leftMap.remove(it)!! to rightMap.remove(it)!!
        }

        return AssociationResult(leftMap.values, rightMap.values, commonValues)
    }

    fun <K> groupBy(getKey: (T) -> K): GroupingResult<K, T> {
        val leftMap = left.groupByTo(mutableMapOf(), getKey)
        val rightMap = right.groupByTo(mutableMapOf(), getKey)
        val commonKeys = leftMap.keys intersect rightMap.keys
        val commonValues = commonKeys.map {
            it to (leftMap.remove(it)!! to rightMap.remove(it)!!)
        }.toMap()

        return GroupingResult(leftMap, rightMap, commonValues)
    }
}

fun <T> Pair<T, T>.compareWith(getProperties: (T) -> Map<String, Comparable<*>>) = run {
    val leftProperties = getProperties(first)
    val rightProperties = getProperties(second)
    check(leftProperties.size == rightProperties.size)
    check(leftProperties.keys == rightProperties.keys)
    val unequalProperties = leftProperties.keys.filter { key ->
        compareValues(leftProperties[key], rightProperties[key]) != 0
    }
    PropertyMatchingResult(first, second, unequalProperties.toSet())
}

fun <T> compare(left: Iterable<T>, right: Iterable<T>) = ComparingList(left, right)
