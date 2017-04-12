package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gong Zhang
 */
public final class ThreadPool {

    @Nullable
    private static ExecutorService pool = Executors.newCachedThreadPool();

    synchronized static void shutdown() {
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }

    public synchronized static void execute(Runnable command) {
        shared().execute(command);
    }

    @NotNull
    public synchronized static Executor shared() {
        if (pool == null) {
            // pool is already shutdown. create new thread instead.
            return command -> new Thread(command).start();
        } else {
            return pool;
        }
    }

}
