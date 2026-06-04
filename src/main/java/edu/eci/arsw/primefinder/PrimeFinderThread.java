package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

/**
 * Thread that searches for prime numbers in the range [a, b).
 * Pauses and resumes according to instructions from {@link Control}.
 */
public class PrimeFinderThread extends Thread {

    int a, b;
    private List<Integer> primes;
    private final Control control;

    /**
     * @param a       start of the range (inclusive)
     * @param b       end of the range (exclusive)
     * @param control pause/resume controller
     */
    public PrimeFinderThread(int a, int b, Control control) {
        super();
        this.primes = new LinkedList<>();
        this.a = a;
        this.b = b;
        this.control = control;
    }

    /**
     * Iterates over the assigned range, checks for pauses, and collects primes found.
     */
    @Override
    public void run() {
        for (int i = a; i < b; i++) {
            try {
                control.checkPause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (isPrime(i)) {
                primes.add(i);
                System.out.println(i);
            }
        }
        control.workerFinished();
    }

    /**
     * Determines whether {@code n} is a prime number.
     *
     * @param n number to evaluate
     * @return {@code true} if {@code n} is prime
     */
    boolean isPrime(int n) {
        boolean ans;
        if (n > 2) {
            ans = n % 2 != 0;
            for (int i = 3; ans && i * i <= n; i += 2) {
                ans = n % i != 0;
            }
        } else {
            ans = n == 2;
        }
        return ans;
    }

    /**
     * @return list of prime numbers found so far
     */
    public List<Integer> getPrimes() {
        return primes;
    }
}
