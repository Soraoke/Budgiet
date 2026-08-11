use std::{format, panic, sync::{Arc, LazyLock, atomic::{AtomicUsize, Ordering}}};
use async_trait::async_trait;
use boltffi::export;
use tokio::{runtime::{Handle, Runtime}, task::JoinError};

/// How many threads are in the Runtime thread-pool.
const NUM_WORKER_THREADS: usize = 3;
/// How many threads (in addition to [`NUM_WORKER_THREADS`]) that are dedicated to running a **blocking** task (aka non-async task).
const NUM_BLOCKING_THREADS: usize = 1;

static RUNTIME_HANDLE: LazyLock<&'static Handle> = LazyLock::new(|| {
    static RUNTIME: LazyLock<Runtime> = LazyLock::new(|| {
        tokio::runtime::Builder::new_multi_thread()
            .name("tokio-worker-runtime")
            .thread_name_fn(|| {
                static ATOMIC_ID: AtomicUsize = AtomicUsize::new(0);
                let id = ATOMIC_ID.fetch_add(1, Ordering::SeqCst);
                format!("worker-thread-{id}")
            })
            .worker_threads(NUM_WORKER_THREADS)
            .max_blocking_threads(NUM_BLOCKING_THREADS)
            .build()
            .unwrap_or_else(|err| panic!("Error constructing tokio Runtime: {err}"))
    });

    RUNTIME.handle()
});

/// Run a **task** in the *default* single worker thread, returning the value that the **`task`** produced.
///
/// > Note: If this function is called from the same thread it's supposed to run on (i.e. *default* worker thread),
/// > it will push the **`task`** to the back of the queue instead of executing it immediately.
///
/// Returns [`Err`] if the **`task`** is *canceled* or causes the worker thread to `panic!`.
pub async fn run_work<T>(task: impl Future<Output = T> + Send + 'static) -> Result<T, JoinError>
where T: Send + 'static {
    RUNTIME_HANDLE.spawn(task).await
}

/// Run an async **task** in the thread-pool.
/// Use this if you don't want to *wait* to return a value when the **`task`** is finished.
///
/// This function [catches][catch_unwind] any `panic!s` produced by the **`task`**,
/// and prints the message to **stderr**.
///
/// > Note: If this function is called from the same thread it's supposed to run on (i.e. *default* worker thread),
/// > it will push the **`task`** to the back of the queue instead of executing it immediately.
pub fn dispatch_work(task: impl Future<Output = ()> + Send + 'static) {
    RUNTIME_HANDLE.spawn(task);
}

/// Same as [`dispatch_work()`], but spawns the **`task`** in the single blocking worker thread.
pub fn dispatch_blocking_work(task: impl FnOnce() + Send + 'static) {
    RUNTIME_HANDLE.spawn_blocking(task);
}

// --- EXPORT FFI functions ---

#[export]
#[async_trait]
#[doc(hidden)]
pub trait AyncCallback: Send + Sync {
    async fn call(&self);
}
#[export]
#[doc(hidden)]
pub trait BlockingCallback: Send + Sync {
    fn call(&self);
}

#[export]
#[doc(hidden)]
/// Run an async **task** in the thread-pool.
/// Use this if you don't want to *wait* to return a value when the **`task`** is finished.
///
/// This function [catches][catch_unwind] any `panic!s` produced by the **`task`**,
/// and prints the message to **stderr**.
///
/// > Note: If this function is called from the same thread it's supposed to run on (i.e. *default* worker thread),
/// > it will push the **`task`** to the back of the queue instead of executing it immediately.
pub fn ffi_dispatch_work(task: Arc<dyn AyncCallback>) {
    dispatch_work(async move { task.call().await });
}
#[export]
#[doc(hidden)]
/// Same as [`dispatch_work()`], but spawns the **`task`** in the single blocking worker thread.
pub fn ffi_dispatch_blocking_work(task: Arc<dyn BlockingCallback>) {
    dispatch_blocking_work(move || task.call());
}
