package app.twicast.tv.core.designsystem

import app.twicast.tv.core.designsystem.component.isFirstInRow
import org.junit.Assert.assertEquals
import org.junit.Test

class SidebarFocusTest {

    @Test
    fun `first item of every row leads to the sidebar`() {
        assertEquals(listOf(0, 8, 16), (0 until 20).filter { isFirstInRow(it, perRow = 8) })
    }

    @Test
    fun `single column grid leads to the sidebar from every item`() {
        assertEquals(listOf(0, 1, 2), (0 until 3).filter { isFirstInRow(it, perRow = 1) })
    }

    @Test
    fun `no columns means no row start`() {
        assertEquals(emptyList<Int>(), (0 until 3).filter { isFirstInRow(it, perRow = 0) })
    }
}
