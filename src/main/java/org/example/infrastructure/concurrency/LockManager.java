package org.example.infrastructure.concurrency;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockManager {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void readWithLock(Runnable task) {
        try {
            lock.readLock().lock();
            task.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeWithLock(Runnable task) {
        try {
            lock.writeLock().lock();
            task.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
