package alien4cloud.deployment;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.SneakyThrows;

@Service
public class DeploymentLockService {

    /**
     * This means that if a plugin deploy and then hold the lock more than 1 day, access to the deployment will not be protected anymore
     * as the new thread which arrive will take a new lock (old lock entry has expired). But a good plugin should not hold a thread for 1 day, the bad ones will
     * consume all the thread pool of the system.
     */
    private LoadingCache<String, ReadWriteLock> lockMap = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS)
            .build(CacheLoader.from(input -> new ReentrantReadWriteLock()));

    public interface ActionWithLock<T> {
        T doAction();
    }

    /**
     * Obtain a write lock on the given deployment and then do action, release the write lock at the end
     * 
     * @param deploymentId id of the deployment
     * @param runnable the action to be executed that requires exclusive write lock
     * @param <T> the return type of the action
     * @return the result of the action
     */
    @SneakyThrows
    public <T> T doWithDeploymentWriteLock(String deploymentId, ActionWithLock<T> runnable) {
        try {
            lockMap.get(deploymentId).writeLock().lock();
            return runnable.doAction();
        } finally {
            lockMap.get(deploymentId).writeLock().unlock();
        }
    }

    /**
     * Obtain a read lock on the given deployment and then do action, release the read lock at the end
     *
     * @param deploymentId id of the deployment
     * @param runnable the action to be executed which requires a read lock
     * @param <T> the return type of the action
     * @return the result of the action
     */
    @SneakyThrows
    public <T> T doWithDeploymentReadLock(String deploymentId, ActionWithLock<T> runnable) {
        try {
            lockMap.get(deploymentId).readLock().lock();
            return runnable.doAction();
        } finally {
            lockMap.get(deploymentId).readLock().unlock();
        }
    }
}
