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
    default ZoneId getLogTimeZone() {
        return ZoneId.systemDefault();
    }

    @NotNull
    default File getLogFile() {
        return new File(".log");
    }

}
