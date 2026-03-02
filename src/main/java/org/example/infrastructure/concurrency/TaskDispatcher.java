package org.example.infrastructure.concurrency;

import lombok.Getter;
import org.example.event.Event;
import org.example.util.DomainServiceUtil;
import org.example.util.EventBus;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class TaskDispatcher {

    private final ExecutorService ioPool = Executors.newVirtualThreadPerTaskExecutor();

    @Getter
    private final ExecutorService cpuPool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    private final Semaphore ioSemaphore = new Semaphore(10);

    private final EventBus eventBus = DomainServiceUtil.getEventBus();

    private static final int IO_TIMEOUT_SECONDS = 10;

    public <T> CompletableFuture<T> submitIo(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        ioSemaphore.acquire();
                        return task.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        eventBus.publish(new Event.CriticalErrorExceptionEvent(e.getCause()));
                        throw new RuntimeException(e);
                    } finally {
                        ioSemaphore.release();
                    }
                }, ioPool)
                .orTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> CompletableFuture<T> runParallel(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            return task.get();
        }, cpuPool);
    }
}
