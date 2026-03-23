package com.example.budgiet

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** An [Executor] containing the *single thread* that will run *blocking tasks*. */
private val WORKER_THREAD = Executors.newSingleThreadScheduledExecutor()
/** The **ID** of the [Thread] in the *single-threaded executor* [WORKER_THREAD].
 *
 * After it is first initialized, the **ID** will not change,
 * because the code it runs will never *throw* an [Exception],
 * so the thread will not terminate until the end of the program.
 *
 * The value does not need to be put in a [Mutex][kotlinx.coroutines.sync.Mutex],
 * as only the worker thread can modify this value. */
private var WORKER_THREAD_ID: Long? = null
@Suppress("FunctionName")
private fun Thread.Id(): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        this.threadId()
    } else {
        @Suppress("DEPRECATION")
        this.id
    }
}
private fun isWorkerThread(): Boolean = WORKER_THREAD_ID != null && Thread.currentThread().Id() == WORKER_THREAD_ID
private fun setWorkerThreadId(executor: Executor) {
    if (executor == WORKER_THREAD && WORKER_THREAD_ID == null) {
        WORKER_THREAD_ID = Thread.currentThread().Id()
    }
}

/** Run a **task** in a *single-threaded* work [Executor],
 * and [remember] the value in a [Composable].
 *
 * This function adds the **task** to the executor and immediately returns a `mutableStateOf(null)`.
 * While the task waits to be executed (and while it is being executed),
 * the *UI* thread can continue the rendering process without having to wait for work to be done.
 *
 * After the **task** is finished, the returned [MutableState] is updated to contain a [Result]:
 * either the *success* value produced by the **task** Callback,
 * or an *error value* if the **task** threw an [Exception] ([Throwable]).
 * Throwing an [Exception] in a [Composable] is not ideal since it will crash the program if not caught,
 * so this function will automatically catch [Exception]s and put it in the [Result] instead.
 *
 * Optionally, the caller can pass a custom [Executor] to run the work in instead of the default **worker thread**. */
@Composable
fun <T> rememberWork(
    executor: Executor = WORKER_THREAD,
    task: suspend () -> T
): MutableState<Result<T>?> {
    val state = remember { mutableStateOf<Result<T>?>(null) }
    suspend fun runTask()
    // Don't allow an exception to terminate the worker thread; gotta catch em all.
            = try {
        Result.Ok(task())
    } catch (e: Throwable) {
        Result.Err(e)
    }

    LaunchedEffect(Unit) {
        withContext(executor.asCoroutineDispatcher()) {
            setWorkerThreadId(executor)

            state.value = runTask()
        }
    }

    return state
}

/** Run a **task** in a *single-threaded* work [Executor],
 * returning the value that the **task** produced.
 *
 * This function adds the **task** to the Executor and *suspends* while waiting for the **task** to produce a result.
 * Like [rememberWork], this function *not rethrow* any [Exception]s thrown by the **task**.
 * Instead, a [Result] is returned.
 *
 * Optionally, the caller can pass a custom [Executor] to run the work in instead of the default **worker thread**.
 *
 * > Note: If this function detects that it is being called from the *default* **worker thread**,
 * > it will just run the *task* in the same thread without first pushing it to the Executor and waiting its turn.
 * > This optimizes the order of running *tasks* in case the caller calls [runWork] without knowing it is in the worker thread,
 * > Although this should be extremely rare. */
suspend fun <T> runWork(executor: Executor = WORKER_THREAD, task: suspend () -> T): Result<T> {
    suspend fun runTask()
    // Don't allow an exception to terminate the worker thread; gotta catch em all.
            = try {
        Result.Ok(task())
    } catch (e: Throwable) {
        Result.Err(e)
    }

    return if (isWorkerThread()) {
        runTask()
    } else {
        withContext(executor.asCoroutineDispatcher()) {
            setWorkerThreadId(executor)

            runTask()
        }
    }
}

/** Run a **task** in a *single-threaded* work [Executor].
 * Use this if you don't want to *wait* for the value returned when the task is finished.
 *
 * Like [rememberWork], this function *not rethrow* any [Exception]s thrown by the **task**.
 * Instead, it will be printed to log.
 *
 * Optionally, the caller can pass a custom [Executor] to run the work in instead of the default **worker thread**.
 *
 * > Note: If this function detects that it is being called from the *default* **worker thread**,
 * > it will just run the *task* in the same thread without first pushing it to the Executor and waiting its turn.
 * > This optimizes the order of running *tasks* in case the caller calls [dispatchWork] without knowing it is in the worker thread,
 * > Although this should be extremely rare. */
fun dispatchWork(executor: Executor = WORKER_THREAD, task: suspend () -> Unit) {
    fun runTask() = runBlocking {
        // Don't allow an exception to terminate the worker thread; gotta catch em all.
        try {
            task()
        } catch (e: Throwable) {
            Log.e("dispatchWork", e.toString())
        }
    }

    if (isWorkerThread()) {
        runTask()
    } else {
        executor.execute {
            setWorkerThreadId(executor)
            runTask()
        }
    }
}
