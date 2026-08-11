package com.example.budgiet.ui.theme

import com.example.budgiet.Color
import androidx.compose.ui.graphics.Color as JColor
import com.example.budgiet.UserColorPalette

val Purple80 = JColor(0xFFD0BCFF)
val PurpleGrey80 = JColor(0xFFCCC2DC)
val Pink80 = JColor(0xFFEFB8C8)

val Purple40 = JColor(0xFF6650a4)
val PurpleGrey40 = JColor(0xFF625b71)
val Pink40 = JColor(0xFF7D5260)

@Suppress("unused")
object UserColorPalette: List<Color> {
    private var list = UserColorPalette.ffiList()

    val Red       = this.list[0]
    val Orange    = this.list[1]
    val Brown     = this.list[2]
    val Yellow    = this.list[3]
    val Green     = this.list[4]
    val Forest    = this.list[5]
    val Turquoise = this.list[6]
    val Cyan      = this.list[7]
    val Blue      = this.list[8]
    val Purple    = this.list[9]
    val Lavender  = this.list[10]
    val Pink      = this.list[11]
    val Grey      = this.list[12]
    val DarkGrey  = this.list[13]

    override val size: Int get() = this.list.size

    override fun isEmpty(): Boolean = this.list.isEmpty()
    override fun contains(element: Color): Boolean = this.list.contains(element)
    override fun iterator(): Iterator<Color> = this.list.iterator()
    override fun containsAll(elements: Collection<Color>): Boolean = this.list.containsAll(elements)
    override fun get(index: Int): Color = this.list[index]
    override fun indexOf(element: Color): Int = this.list.indexOf(element)
    override fun lastIndexOf(element: Color): Int = this.list.lastIndexOf(element)
    override fun listIterator(): ListIterator<Color> = this.list.listIterator()
    override fun listIterator(index: Int): ListIterator<Color> = this.list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Color> = this.list.subList(fromIndex, toIndex)
}
