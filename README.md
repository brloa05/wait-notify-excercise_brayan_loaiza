# PrimeFinder — Multithreaded Prime Number Search

A Java application that finds all prime numbers between 0 and 30,000,000 using three parallel worker threads. Every 5 seconds the workers are paused, the current count of primes found is displayed, and execution resumes only after the user presses ENTER.

---

## How to run

```bash
mvn compile exec:java
```

Requirements: Java 8+, Maven 3+.

---

## Project structure

```
src/main/java/edu/eci/arsw/primefinder/
├── Main.java              # Entry point
├── Control.java           # Supervisor thread: manages workers and pause cycles
└── PrimeFinderThread.java # Worker thread: searches primes in a numeric range
```

---

## How it works

The range [0, 30,000,000] is split evenly across three worker threads:

| Thread | Range                    |
|--------|--------------------------|
| Worker 0 | 0 to 9,999,999           |
| Worker 1 | 10,000,000 to 19,999,999 |
| Worker 2 | 20,000,000 to 30,000,000 |

All three run simultaneously. The `Control` thread wakes up every 5 seconds, pauses all workers, prints how many primes have been found so far, and waits for the user to press ENTER before resuming. When all workers finish their ranges, the final total is printed and the program exits.

---

## Synchronization design

### The lock

A single shared monitor object is used for all synchronization:

```java
final Object lock = new Object();
```

Every read and write of the shared state variables (`paused`, `pausedWorkers`, `activeWorkers`) happens inside a `synchronized(lock)` block. This guarantees that only one thread modifies these variables at a time, preventing race conditions.

### Shared state variables

| Variable | Purpose |
|----------|---------|
| `paused` | Flag that signals workers to stop at their next checkpoint |
| `pausedWorkers` | How many workers are currently sleeping in `wait()` |
| `activeWorkers` | How many workers have not yet finished their range |

### The pause/resume cycle

**Control signals a pause:**
```java
synchronized (lock) {
    paused = true;
    while (pausedWorkers < activeWorkers) {
        lock.wait(); // waits until every active worker is sleeping
    }
}
```
`Control` sets `paused = true` and then blocks until all active workers have entered `wait()`. The condition is re-checked every time a worker notifies that it has paused.

**Workers pause themselves (`checkPause()`):**
```java
synchronized (lock) {
    while (paused) {
        pausedWorkers++;
        lock.notifyAll(); // wakes Control so it can re-check the counter
        lock.wait();      // releases the lock and sleeps
        pausedWorkers--;  // decremented once Control resumes them
    }
}
```
Each worker calls `checkPause()` on every loop iteration. If `paused` is `true`, it increments the counter, notifies `Control`, and goes to sleep — releasing the lock so other threads can enter.

**Control resumes all workers:**
```java
synchronized (lock) {
    paused = false;
    lock.notifyAll(); // wakes every worker waiting on this lock
}
```
After the user presses ENTER, `Control` clears the flag and calls `notifyAll()`. Every sleeping worker wakes up, re-checks the condition (`paused == false`), and continues searching.

---

## How lost wakeups are prevented

A **lost wakeup** happens when a thread misses a `notify()` and sleeps forever. This design prevents them through three mechanisms:

**1. `while` instead of `if` around every `wait()` call**

Both workers and `Control` always re-check their condition after waking up:
```java
while (paused) { lock.wait(); }             // worker side
while (pausedWorkers < activeWorkers) { lock.wait(); } // control side
```
If a thread wakes up spuriously (which Java allows by design), it re-evaluates the condition before continuing. No thread ever proceeds based on a stale state.

**2. `notifyAll()` instead of `notify()`**

`notify()` wakes only one arbitrary thread. If the wrong thread is woken, the right one stays asleep indefinitely. `notifyAll()` wakes every thread waiting on the lock, so all of them get a chance to re-check their condition.

**3. The `paused` flag persists until explicitly cleared**

`paused` is set to `true` before any worker might check it, and stays `true` until `Control` explicitly clears it. A worker that arrives late at `checkPause()` will always see the flag and enter `wait()` correctly — it cannot miss a wakeup because no wakeup has been sent yet.

---

## Observations

- **One lock is enough**: having a single shared lock for all threads keeps the design simple and easy to reason about. With more locks, threads could end up waiting on each other in a cycle and freeze the program entirely — using just one avoids that problem completely.

- **Workers that finish early do not break the pause**: if a worker finishes searching its entire range before a pause is triggered, the program still behaves correctly. The supervisor simply adjusts its count of active workers and moves on without getting stuck waiting for someone who is already done.

- **Pausing feels almost instant**: because every worker checks whether it should pause on each number it processes, the delay between the supervisor requesting a pause and all workers actually stopping is extremely small — practically unnoticeable to the user.
