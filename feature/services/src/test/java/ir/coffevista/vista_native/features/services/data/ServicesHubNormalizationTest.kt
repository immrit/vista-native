package ir.coffevista.vista_native.features.services.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ServicesHubNormalizationTest {
    @Test
    fun `keeps first active section for a duplicate route`() {
        val result = normalizedActiveSections(
            listOf(
                section(id = 2, title = "فروشگاه", route = "vista://store", order = 2),
                section(id = 1, title = "فروشگاه", route = "vista://store", order = 1),
                section(id = 3, title = "مسابقات", route = "vista://competition", order = 3),
                section(id = 4, title = "غیرفعال", route = "vista://inactive", order = 0, active = false),
            ),
        )

        assertEquals(listOf(1L, 3L), result.map(ServiceSection::id))
    }

    private fun section(
        id: Long,
        title: String,
        route: String,
        order: Int,
        active: Boolean = true,
    ) = ServiceSection(
        id = id,
        title = title,
        subtitle = "",
        icon = "grid",
        route = route,
        color = "#6366F1",
        isActive = active,
        sortOrder = order,
    )
}
