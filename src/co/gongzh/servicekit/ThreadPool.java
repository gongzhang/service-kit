package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gong Zhang
 */
public final class ThreadPool {

    @Nullable
    private static ExecutorService globalPool = null;

    synchronized static void initGlobal(@NotNull ExecutorService pool) {
        ThreadPool.globalPool = pool;
    }

    synchronized static ExecutorService getGlobalPool() {
        return globalPool;
    }

    public synchronized static void execute(@NotNull Runnable command) {
        if (globalPool != null) {
            globalPool.execute(command);
        }
    }

    @NotNull
    public synchronized static Executor global() {
        if (globalPool == null) {
            // pool is already shutdown. returns placeholder.
            return command -> {};
        } else {
            return globalPool;
        }
    }

}
