package io.hugogu.matchito

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class CollectionComparingTest {
    @Test
    fun associateByTest() {
        val left = listOf(1, 2, 3)
        val right = listOf(2, 3, 5)
        val association = compare(left, right).associateBy { it }
        assertThat(association.leftOnly, contains(1))
        assertThat(association.rightOnly, contains(5))
        assertThat(association.matches.values, contains(
            ValuePair(2, 2),
            ValuePair(3, 3)
        ))
    }

    @Test
    fun matchByTest() {
        val left = listOf(
            Person("A", LocalDate.parse("2020-04-05")),
            Person("B", LocalDate.parse("2020-04-05")),
            Person("C", LocalDate.parse("2020-04-05"))
        )
        val right = listOf(
            Person("a", LocalDate.parse("2020-04-05"), left[0].id),
            Person("B", LocalDate.parse("2020-04-06"), left[1].id),
            Person("C", LocalDate.parse("2020-04-05"), left[2].id)
        )
        val association = compare(left, right).associateBy { it.id }
        assertThat(association.leftOnly, hasSize(0))
        assertThat(association.rightOnly, hasSize(0))
        val matches = association.thenMatchProperties(
            { "name" to it.name },
            { "birthday" to it.birthday }
        )
        assertThat(matches.leftOnly, hasSize(0))
        assertThat(matches.rightOnly, hasSize(0))
        assertThat(matches.fullMatches, hasSize(1))
        assertThat(matches.fullMatches.flatMap { it.unequalProperties.entries }, hasSize(0))
        assertThat(matches.partialMatches, hasSize(2))
        assertThat(
            matches.partialMatches, containsInAnyOrder(
                hasProperty(
                    "unequalProperties",
                    hasEntry("name", ValuePair("A", "a"))
                ),
                hasProperty(
                    "unequalProperties",
                    hasEntry("birthday", ValuePair(LocalDate.parse("2020-04-05"), LocalDate.parse("2020-04-06")))
                )
            )
        )
    }

    data class Person(
        val name: String,
        val birthday: LocalDate,
        val id: UUID = UUID.randomUUID()
    )
}
