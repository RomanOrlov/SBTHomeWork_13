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
    private volatile boolean stopped;
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
            while (!stopped) {
                Runnable task;
                synchronized (lock) {
                    while (tasks.isEmpty()) {
                        if (stopped || busyThreadsCount > min) {
                            workers.remove(ScalableThread.this);
                            System.err.println("removing "+busyThreadsCount);
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
                System.err.println("dec " + busyThreadsCount);
            }
        }

        private void incrementBusyThreads() {
            synchronized (lock) {
                busyThreadsCount++;
                System.err.println("inc " + busyThreadsCount);
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
        if (workers.size() < max && !workers.isEmpty() && busyThreadsCount == workers.size()) {
            ScalableThread scalableThread = new ScalableThread("Sad thread, must work harder");
            workers.add(scalableThread);
            System.err.println("add" + busyThreadsCount);
            scalableThread.start();
        }else if (workers.size() < max && !started) {
            ScalableThread scalableThread = new ScalableThread("Sad thread, must work harder");
            workers.add(scalableThread);
        }
    }

    @Override
    public void start() {
        started = true;
        workers.forEach(Thread::start);
    }

    public void stop() {
        stopped = true;
    }
}
