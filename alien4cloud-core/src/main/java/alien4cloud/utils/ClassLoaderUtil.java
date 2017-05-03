package alien4cloud.utils;

import java.util.function.Supplier;

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

    public static <T> T getWithContextClassLoader(ClassLoader classLoader, Supplier<T> supplier) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
