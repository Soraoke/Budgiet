package com.example.budgiet

import androidx.compose.foundation.text.input.TextFieldState
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import junit.framework.AssertionFailedError
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.collections.listOf
import kotlin.math.min

class PagingUnitTests {
    /** How many **items** a page has.
     * In this case, this is analogous to how many items are visible,
     * however, in the UI only a *portion* of a page is visible to the user. */
    val pageSize = 4u
    /** How many **pages** can be loaded at a time. */
    val maxPages = 3u

    fun <T: Any> newPager(source: ListPagingSource<T>) = TestPager(
        config = PagingConfig(
            // These config values are only for testing, and are very different in production.
            pageSize = pageSize.toInt(),
            initialLoadSize = pageSize.toInt(),
            // Only load pages that are actually requested (doesn't seem to work).
            prefetchDistance = pageSize.toInt() - 1,
            maxSize = (maxPages * pageSize).toInt(),
        ),
        pagingSource = source,
    )

    /** Generate a [List] that contains *at most* the number of items that can fit in **numPages**.
     *
     * The greatest (last) item can only be as high as the number of items that fit in **numPages**.
     * aka `list.last() < numPages * pageSize`.
     *
     * @param maxItem The greatest **item** ([Int]) that can be generated -1.
     * @param start The *first* item in the [List].
     *        It is also a way to tell what page it is on.
     * @param length How many items are requested for this specific page. */
    fun genList(maxItem: UInt?, start: UInt, length: UInt): List<Int> {
        if (maxItem == null)
            return List(length.toInt()) { i -> start.toInt() + i }

        // start index is out of bounds
        if (start > maxItem)
            return listOf()

        // List should only contain items within the number of pages that it can generate
        return List(min(length, maxItem - start).toInt())
            { i -> start.toInt() + i }
    }

    /** Generates a *2-dimensional* [List] of **page items** ([Int]).
     * This list represents the **pages** that are expected to be loaded in the [TestPager].
     *
     * @param start The *first* item in the *first page*.
     * @param length How many items ([Int]) should be generated in total.
     *   The *last* item in the *last page* is `start + length - 1`. */
    fun expectedPages(start: UInt = 0u, length: UInt): List<List<Int>>
        = (start.toInt()..<(start + length).toInt()).toList()
            .chunked(pageSize.toInt())

    /** Same as [TestPager.getPages]. */
    suspend fun <T: Any> TestPager<PagingKey, T>.pages(): List<List<T>>
        = this.getPages()
            .map { page -> page.toList() }

    @Test
    fun pageByPageTest() {
        // Keep a list of what pages get created in load()
        val generatedPages = mutableListOf<List<Int>>()
        // Pager loads infinite pages
        val infinitePager = newPager(ListPagingSource.withoutQuery { start, length ->
            val page = genList(null, start, length)
            generatedPages.add(page)
            page
        })

        runBlocking {
            infinitePager.refresh(0u) // refresh() must always be called first.

            // Load the first page.
            // infinitePager.append()!!
            assertEquals(
                actual = infinitePager.pages(),
                // Only one page should be loaded.
                expected = expectedPages(0u, pageSize),
            )
            // Check that no other pages were secretly loaded.
            assertEquals(generatedPages.size, 1)

            // Load the second page.
            infinitePager.append()!!
            assertEquals(
                actual = infinitePager.pages(),
                // Only 2 pages should be loaded.
                expected = expectedPages(0u, pageSize * 2u),
            )
            // Check that no other pages were secretly loaded.
            assertEquals(2, generatedPages.size)

            // Load the third page (max loaded).
            infinitePager.append()!!
            assertEquals(
                actual = infinitePager.pages(),
                // Only 3 pages should be loaded.
                expected = expectedPages(0u, pageSize * maxPages),
            )
            // Check that no other pages were secretly loaded.
            assertEquals(3, generatedPages.size)

            // Load the fourth page (unload first page).
            infinitePager.append()!!
            assertEquals(
                actual = infinitePager.pages(),
                // Only 3 pages should be loaded.
                expected = expectedPages(pageSize, pageSize * maxPages),
            )
            // Check that no other pages were secretly loaded.
            assertEquals(4, generatedPages.size)

            // Reload the first page (last page should be unloaded).
            infinitePager.prepend()!!
            assertEquals(
                actual = infinitePager.pages(),
                // Only 3 pages should be loaded.
                expected = expectedPages(0u, pageSize * maxPages),
            )
            // Check that no other pages were secretly loaded.
            assertEquals(5, generatedPages.size)
        }
    }

    /** Test that the Pager stops trying to load items when the data size is smaller than the page size. */
    @Test
    fun smallerDataSize() {
        /** How many pages should be full pages (i.e. contain the amount of items requested).
         *
         * Use maxPages to avoid unload of first page.
         * That is tested in pageByPageTest(). */
        val numFullPages = maxPages - 1u
        val partialPageSize = pageSize - 2u
        val smallerPager = newPager(ListPagingSource.withoutQuery { start, length ->
            val maxItem = pageSize * (numFullPages + 1u)
            if (start >= numFullPages * pageSize) {
                // For the last page, return less data than requested.
                genList(maxItem, start, partialPageSize)
            } else {
                genList(maxItem, start, length)
            }
        })

        runBlocking {
            smallerPager.refresh(0u) // refresh() must always be called first.

            // Load the last full page (should all be full pages).
            // First page is always loaded first, so .append() loads the second page.
            repeat((numFullPages - 1u).toInt()) { _ -> smallerPager.append()!! }
            assertEquals(
                actual = smallerPager.pages(),
                expected = expectedPages(0u, pageSize * numFullPages)
            )

            // Load the last page (should be partially filled).
            smallerPager.append()!!
            assertEquals(
                actual = smallerPager.pages(),
                expected = expectedPages(0u, (pageSize * numFullPages) + partialPageSize)
            )

            // Try to load past last page (should be the same).
            assertEquals(null, smallerPager.append())
            assertEquals(
                actual = smallerPager.pages(),
                expected = expectedPages(0u, (pageSize * numFullPages) + partialPageSize)
            )
        }
    }

    /** Test that the Pager keeps trying to load items when the data size is the same as the page size.
     * The pager should also stop trying to load items when the data comes up empty. */
    @Test
    fun exactDataSize() {
        /** How many pages should be generated.
         *
         * Use maxPages to avoid unload of first page.
         * That is tested in pageByPageTest(). */
        val numPages = maxPages - 1u
        val exactPager = newPager(ListPagingSource.withoutQuery { start, length ->
            genList(pageSize * numPages, start, length)
        })

        runBlocking {
            exactPager.refresh(0u) // refresh() must always be called first.

            // Load the last page.
            // First page is always loaded first, so .append() loads the second page.
            repeat((numPages - 1u).toInt()) { _ -> exactPager.append()!! }
            assertEquals(
                actual = exactPager.pages(),
                expected = expectedPages(0u, pageSize * numPages)
            )

            // Try to load next page (returns empty data)
            exactPager.append()!!
            assertEquals(
                actual = exactPager.pages(),
                expected = expectedPages(0u, pageSize * numPages) + listOf(listOf())
            )

            // Try to load past last page (should be the same).
            assertEquals(null, exactPager.append())
            assertEquals(
                actual = exactPager.pages(),
                expected = expectedPages(0u, pageSize * numPages) + listOf(listOf())
            )
        }
    }

    /** Test that the Pager ignores last items when the data size is larger than the page size. */
    @Test
    fun largerDataSize() {
        /** How many pages should have the exact amount of items requested.
         *
         * Use maxPages to avoid unload of first page.
         * That is tested in pageByPageTest(). */
        val numExactPages = maxPages - 1u
        /** How many extra items should be generated for the last page. */
        val extraPageSize = 3u // Arbitrary value
        val largerPager = newPager(ListPagingSource.withoutQuery { start, length ->
            val maxItem = (pageSize * numExactPages) + extraPageSize
            if (start == numExactPages * pageSize) {
                // For the second-to-last page, return more data than requested.
                genList(maxItem, start, length + extraPageSize)
            } else if (start > numExactPages * pageSize) {
                // For the last page, return the data that was ignored
                genList(maxItem, start, extraPageSize)
            } else {
                genList(maxItem, start, length)
            }
        })

        runBlocking {
            largerPager.refresh(0u) // refresh() must always be called first.

            // Load the first normal pages (should load exact data).
            // First page is always loaded first, so .append() loads the second page.
            repeat((numExactPages - 2u).toInt()) { _ -> largerPager.append()!! }
            assertEquals(
                actual = largerPager.pages(),
                expected = expectedPages(0u, pageSize * (numExactPages - 1u))
            )

            // Load the last normal page (should ignore extra items).
            largerPager.append()!!
            assertEquals(
                actual = largerPager.pages(),
                expected = expectedPages(0u, pageSize * numExactPages)
            )

            // Load the next page (should include the extra items).
            largerPager.append()!!
            assertEquals(
                actual = largerPager.pages(),
                expected = expectedPages(0u, (pageSize * numExactPages) + extraPageSize)
            )

            // Try to load past last page (should be the same).
            assertEquals(null, largerPager.append())
            assertEquals(
                actual = largerPager.pages(),
                expected = expectedPages(0u, (pageSize * numExactPages) + extraPageSize)
            )
        }
    }

    @Test
    fun emptyDataSize() {
        val emptyPager = newPager(ListPagingSource.withoutQuery { _, _ -> listOf<Int>() })

        runBlocking {
            emptyPager.refresh(0u)
                .assert({ it is LoadResult.Page && it.data.isEmpty() })
            assertEquals(emptyPager.pages(), listOf(listOf<Int>()))

            emptyPager.append().assertEquals(null)
            assertEquals(emptyPager.pages(), listOf(listOf<Int>()))
        }
    }

    /** Tests that the pager handles when the data loader throws an Exception, and it doesn't throw outside the pager. */
    @Test
    fun exceptionTest() {
        /** Returns a good first page, bad second page, and good third page. */
        fun loadData(start: UInt, length: UInt): List<Int> {
            val maxItem = pageSize * 3u
            return if (start < pageSize) {
                genList(maxItem, start, length)
            } else if (pageSize <= start && start < pageSize * 2u) {
                throw Exception("2nd page exception")
            } else {
                genList(maxItem, start, length)
            }
        }
        var exceptionPager = newPager(ListPagingSource.withoutQuery { start, length ->
            loadData(start, length)
        })

        runBlocking {
            assert(exceptionPager.refresh(0u) is LoadResult.Page)
            assert(exceptionPager.append()!! is LoadResult.Error)

            // Do the same for pager with query, as the implementation could try skipping the catcher.
            exceptionPager = newPager(ListPagingSource.withQuery(
                getPage = { _, start, length -> loadData(start, length) },
                queryState = TextFieldState()
            ))

            assert(exceptionPager.refresh(0u) is LoadResult.Page)
            assert(exceptionPager.append()!! is LoadResult.Error)
        }
    }

    // This seems fundamentally impossible to test because the suspend primitives callable from a non-suspend block will block until the coroutine is finished,
    // or will  create a Job in another thread, which defeats the purpose of checking if the PagingSource executes the loader in a thread other than the current thread.
    // We just have to trust that the UI thread will not be blocked while the worker thread executes the loader.
    //
    // /** Test that load does not block the UI thread. */
    // @Test
    // fun blockThreadTest() {
    //     val sleepingTime = 5000L
    //     val sleepingPager = newPager(ListPagingSource.withoutQuery { start, length ->
    //         Thread.sleep(sleepingTime)
    //         genList(pageSize, start, length)
    //     })
    //
    //     lateinit var job: Job
    //     val spawnDuration = measureTime {
    //         runBlocking {
    //             job = async { sleepingPager.refresh(0u) }.job
    //         }
    //     }
    //     assert(spawnDuration.inWholeMilliseconds < sleepingTime)
    //
    //     // Check that the job was actually running
    //     assert(job.isActive)
    //
    //     // Do the same for pager with query, as the implementation could try skipping the runner.
    // }

    /** Testing that the logic for genList() works */
    @Test
    fun genListTest() {
        val numPages = maxPages
        val maxItem = numPages * pageSize
        // Requested list with exactly enough items to fit in the page
        var list = genList(maxItem, 0u, numPages * pageSize)
        assertEquals(
            actual = list,
            expected = (0..<(numPages * pageSize).toInt()).toList(),
        )
        list = genList(maxItem, 4u, 4u)
        assertEquals(
            actual = list,
            expected = (4..7).toList(),
        )
        // Requested a list where all items are outside the pages bound
        list = genList(maxItem, numPages * pageSize, 9u)
        assertEquals(
            actual = list,
            expected = listOf(),
        )
        // Requested a list where some items are outside the pages bound
        list = genList(maxItem, 6u, 9999u)
        assertEquals(
            actual = list,
            expected = (6..<(numPages * pageSize).toInt()).toList()
        )
        list = genList(0u, 6u, 9u)
        assertEquals(
            actual = list,
            expected = listOf()
        )
    }
}

// WHY ISN'T THERE AN IMPLEMENTATION OF THIS FOR LIST
fun <T> assertEquals(actual: List<T>, expected: List<T>) {
    fun <T> isEqual(one: List<T>, two: List<T>): Boolean {
        if (one.size != two.size)
            return false

        for (i in 0..<one.size) {
            if (one[i] != two[i])
                return false
        }

        return true
    }

    if (!isEqual(actual, expected))
        // FIXME: why message prints twice
        throw AssertionFailedError("Assertion failed:\n\texpected = $expected\n\tactual = $actual")
}
