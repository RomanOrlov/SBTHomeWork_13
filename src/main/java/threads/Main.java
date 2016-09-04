package threads;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ScalableThreadPool threadPool = new ScalableThreadPool(2, 5);
        for (int i = 0; i < 10; i++) {
            final int ii = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("Task " + ii);
            });
        }
        threadPool.start();
        Thread.sleep(5000);
        System.out.println();
        System.out.println();
        for (int i = 20; i < 35; i++) {
            final int ii = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("Task " + ii);
            });
        }

    }
}
