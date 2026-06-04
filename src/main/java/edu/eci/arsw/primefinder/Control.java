package edu.eci.arsw.primefinder;

import java.util.Scanner;

/**
 * Controller thread that manages the prime-finder worker threads.
 * Every {@value #TMILISECONDS} ms it pauses all workers, prints progress,
 * and waits for the user to press ENTER before resuming them.
 */
public class Control extends Thread {

    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;
    private final PrimeFinderThread[] pft;

    /** Shared monitor used to synchronize pause and resume operations. */
    final Object lock = new Object();
    private boolean paused = false;
    private int pausedWorkers = 0;
    private int activeWorkers = NTHREADS;

    private Control() {
        super();
        pft = new PrimeFinderThread[NTHREADS];
        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, this);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, this);
    }

    /**
     * Creates and initializes a new {@code Control} instance.
     *
     * @return a new instance ready to start
     */
    public static Control newControl() {
        return new Control();
    }

    /**
     * Starts the worker threads and enters the periodic pause loop.
     * Exits when all workers have finished.
     */
    @Override
    public void run() {
        for (PrimeFinderThread t : pft) t.start();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                Thread.sleep(TMILISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            int active;
            synchronized (lock) {
                if (activeWorkers == 0) {
                    int total = 0;
                    for (PrimeFinderThread t : pft) total += t.getPrimes().size();
                    System.out.println("\n[DONE] Total primes found: " + total);
                    break;
                }

                paused = true;
                while (pausedWorkers < activeWorkers) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        paused = false;
                        lock.notifyAll();
                        return;
                    }
                }
                active = activeWorkers;
            }

            int total = 0;
            for (PrimeFinderThread t : pft) total += t.getPrimes().size();
            System.out.println("\n[PAUSED] Primes found so far: " + total);

            if (active > 0) {
                System.out.println("Press ENTER to resume...");
                scanner.nextLine();
                synchronized (lock) {
                    paused = false;
                    lock.notifyAll();
                }
            } else {
                System.out.println("All workers finished.");
                break;
            }
        }
    }

    /**
     * Called by each worker on every iteration; blocks the thread while
     * the controller is in a paused state.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void checkPause() throws InterruptedException {
        synchronized (lock) {
            while (paused) {
                pausedWorkers++;
                lock.notifyAll();
                lock.wait();
                pausedWorkers--;
            }
        }
    }

    /**
     * Called by a worker when it finishes its range; decrements the active
     * worker count and notifies the controller.
     */
    void workerFinished() {
        synchronized (lock) {
            activeWorkers--;
            lock.notifyAll();
        }
    }
}
