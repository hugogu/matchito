package io.hugogu.matchito

import org.hamcrest.Matchers.contains
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class CollectionComparingTest {
    @Test
    fun associateByTest() {
        val left = listOf(1, 2, 3)
        val right = listOf(2, 3, 5)
        val association = compare(left, right).associateBy { it }
        assertThat(association.leftOnly, contains(1))
        assertThat(association.rightOnly, contains(5))
        assertThat(association.matches, contains(2 to 2, 3 to 3))
    }
}