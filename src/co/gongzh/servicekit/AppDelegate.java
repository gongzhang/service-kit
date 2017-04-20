package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.ZoneId;

/**
 * @author Gong Zhang
 */
public interface AppDelegate {

    void onStart();
    void onStop();
    void onCommand(@NotNull String line);

    @NotNull
    default LogFileResolver getLogFileResolver() {
        return () -> new File(".log");
    }

}
