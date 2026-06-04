package edu.eci.arsw.primefinder;

/**
 * Entry point of the prime finder application.
 */
public class Main {

    /**
     * Creates the controller, starts the search, and waits for it to finish.
     *
     * @param args command-line arguments (not used)
     * @throws InterruptedException if the main thread is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        Control control = Control.newControl();
        control.start();
        control.join();
    }
}
