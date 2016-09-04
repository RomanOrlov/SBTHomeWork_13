package threads;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScalableThreadPool implements ThreadPool {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Object lock = new Object();
    private final List<Thread> workers;
    private final Logger logger = Logger.getGlobal();
    private final int min;
    private final int max;
    private volatile int busyThreadsCount;
    private volatile boolean started;

    public ScalableThreadPool(int min, int max) {
        this.min = min;
        this.max = max;
        workers = new ArrayList<>(min);
        for (int i = 0; i < min; i++) {
            workers.add(new ScalableThread("I am happy thread number " + i));
        }
    }

    private class ScalableThread extends Thread {
        public ScalableThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                Runnable task;
                synchronized (lock) {
                    while (tasks.isEmpty()) {
                        if (workers.size() > min) {
                            workers.remove(ScalableThread.this);
                            System.err.println("removing " + busyThreadsCount + " current size " + workers.size());
                            return;
                        }
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    task = tasks.poll();
                    incrementBusyThreads();
                }
                try {
                    task.run();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                } finally {
                    decrementBusyThreads();
                }
            }
        }

        private void decrementBusyThreads() {
            synchronized (lock) {
                busyThreadsCount--;
                System.err.println("dec busy " + busyThreadsCount + " task size " + tasks.size());
            }
        }

        private void incrementBusyThreads() {
            synchronized (lock) {
                busyThreadsCount++;
                System.err.println("inc busy " + busyThreadsCount + " task size " + tasks.size());
            }
        }
    }


    @Override
    public void execute(Runnable runnable) {
        synchronized (lock) {
            scalePool();
            tasks.add(runnable);
            lock.notify();
        }
    }

    private void scalePool() {
        if (!started && workers.size() < max) {
            ScalableThread scalableThread = new ScalableThread("Sad thread, must work harder");
            workers.add(scalableThread);
            System.err.println("add before start " + busyThreadsCount);
        } else if (busyThreadsCount == min) {
            ScalableThread scalableThread = new ScalableThread("Sad thread, must work harder");
            workers.add(scalableThread);
            System.err.println("add after start" + busyThreadsCount);
            scalableThread.start();
        }
    }

    @Override
    public void start() {
        started = true;
        workers.forEach(Thread::start);
    }
}
