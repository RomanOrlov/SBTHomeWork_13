package threads;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FixedThreadPool implements ThreadPool {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Object lock = new Object();
    private final List<Thread> workers;
    private final Logger logger = Logger.getGlobal();

    public FixedThreadPool(int threadCount) {
        workers = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            workers.add(new FixedThread("I am happy thread number " + i));
        }
    }

    private class FixedThread extends Thread {
        public FixedThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                Runnable task;
                synchronized (lock) {
                    while (tasks.isEmpty()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    task = tasks.poll();
                }
                try {
                    task.run();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public void start() {
        workers.forEach(Thread::start);
    }

    @Override
    public void execute(Runnable runnable) {
        synchronized (lock) {
            tasks.add(runnable);
            lock.notify();
        }
    }
}