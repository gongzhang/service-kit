package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.ZoneId;

/**
 * @author Gong Zhang
 */
public interface LogFileResolver {

    /**
     * Do initialization. Client implementation should keep
     * a reference on <code>onLogFileChange</code>.
     * @param onLogFileChange an event that used to notify log engine switching
     *                        new log file.
     */
    default void onCreate(@NotNull EventDispatch<File> onLogFileChange) {}

    /**
     * Called just before a new log is being appended to log file.
     * Client implementation should check current time and determine
     * whether should switch to a new log file or not. If the answer
     * is yes, fire the <code>onLogFileChange</code> event to notify
     * log engine.
     */
    default void onLog() {}

    /**
     * Returns the log file that currently using.
     * @return the current log file.
     */
    @NotNull
    File getCurrentLogFile();

    @NotNull
    default ZoneId getLogZoneId() {
        return ZoneId.systemDefault();
    }

}
