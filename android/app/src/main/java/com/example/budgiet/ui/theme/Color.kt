package com.example.budgiet.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Suppress("unused")
object UserColorPalette: List<Color> {
    val Red = Color(0xFFF3413D)
    val Orange = Color(0xFFFA7B40)
    val Brown = Color(0xFFB37200)
    val Yellow = Color(0xFFF3E248)
    val Green = Color(0xFF21BF13)
    val Forest = Color(0xFF00966E)
    val Turquoise = Color(0xFF37FDAD)
    val Cyan = Color(0xFF34F6FA)
    val Blue = Color(0xFF3D50F3)
    val Purple = Color(0xFFAA07FF)
    val Lavender = Color(0xFFD88FFF)
    val Pink = Color(0xFFFF84EF)
    val Grey = Color(0xFFCFCFCF)
    val DarkGrey = Color(0xFF6B6B6B)

    private var list = listOf(
        this.Red, this.Orange, this.Brown, this.Yellow,
        this.Green, this.Forest, this.Turquoise, this.Cyan,
        this.Blue, this.Purple, this.Lavender, this.Pink,
        this.Grey, this.DarkGrey
    )

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
