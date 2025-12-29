package org.hestiastore.index.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThredTestMain {

    public static void main(String[] args) {
        System.out.println("Starting!");

        ExecutorService executorService = new ThreadPoolExecutor(4, 10, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        Runnable runnableTask = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(300);
                System.out.println("Runnable task's execution - done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        Callable<String> callableTask = () -> {
            TimeUnit.MILLISECONDS.sleep(300);
            System.out.println("Callable task's execution - done");
            return "Task's execution";
        };

        List<Callable<String>> callableTasks = new ArrayList<>();
        callableTasks.add(callableTask);
        callableTasks.add(callableTask);
        callableTasks.add(callableTask);

        for (int i = 0; i < 3; i++) {
            Future<String> future = executorService.submit(callableTask);
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        System.out.println("Done!");
    }
}
