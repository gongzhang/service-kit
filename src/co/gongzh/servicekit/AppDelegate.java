package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gong Zhang
 */
public interface AppDelegate {

    /**
     * @return status code. (<code>0</code> means the app is successfully started, while other
     *         values are error code that will be returned to system.)
     */
    int onStart();
    void onStop();
    void onCommand(@NotNull String line);

    default void onStdinClose() {}

    @NotNull
    default LogFileResolver getLogFileResolver() {
        return () -> new File(".log");
    }

    default ExecutorService createGlobalThreadPool() {
        return Executors.newCachedThreadPool();
    }

    default void purgeGlobalThreadPool(@NotNull ExecutorService pool) {
        pool.shutdownNow();
    }

}
