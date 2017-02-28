package alien4cloud.deployment;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

@Service
public class DeploymentLockService {

    private ReentrantReadWriteLock deployLock = new ReentrantReadWriteLock();

    public interface ActionWithLock<T> {
        T doAction();
    }

    public <T> T doWithDeploymentWriteLock(ActionWithLock<T> runnable) {
        try {
            deployLock.writeLock().lock();
            return runnable.doAction();
        } finally {
            deployLock.writeLock().unlock();
        }
    }

    public <T> T doWithDeploymentReadLock(ActionWithLock<T> runnable) {
        try {
            deployLock.readLock().lock();
            return runnable.doAction();
        } finally {
            deployLock.readLock().unlock();
        }
    }
}
