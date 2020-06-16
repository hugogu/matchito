package io.hugogu.matchito

data class AssociationResult<T>(
    val leftOnly: Iterable<T>,
    val rightOnly: Iterable<T>,
    val matches: Iterable<Pair<T, T>>
)

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

fun <T> compare(left: Iterable<T>, right: Iterable<T>) = ComparingList(left, right)
