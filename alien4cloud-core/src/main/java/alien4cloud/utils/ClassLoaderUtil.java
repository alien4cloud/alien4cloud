package alien4cloud.utils;

public class ClassLoaderUtil {

    public static void runWithContextClassLoader(ClassLoader classLoader, Runnable runnable) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public interface Job<T> {
        T doJob();
    }

    public static <T> T runWithContextClassLoader(ClassLoader classLoader, Job<T> job) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return job.doJob();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
