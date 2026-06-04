package edu.eci.arsw.primefinder;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Control control = Control.newControl();
        control.start();
        control.join();
    }
}
