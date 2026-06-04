package edu.eci.arsw.primefinder;

import java.util.Scanner;

public class Control extends Thread {

    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;
    private final PrimeFinderThread[] pft;


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

    public static Control newControl() {
        return new Control();
    }

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


    void workerFinished() {
        synchronized (lock) {
            activeWorkers--;
            lock.notifyAll();
        }
    }
}
