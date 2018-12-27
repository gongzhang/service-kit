package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gong Zhang
 */
public final class Log {

    private static Log shared = null;
    private static LogFileResolver logFileResolver = null;
    private static EventDispatch<File> onLogFileChange = null;
    private static Filter filter = null;

    @FunctionalInterface
    public interface Filter {
        boolean accept(@NotNull OffsetDateTime time, char level, @NotNull String tag, @NotNull String message);
    }

    @NotNull
    private static DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, false)
            .appendOffsetId()
            .toFormatter(Locale.US);

    @NotNull
    public static DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public static void setDateTimeFormatter(@NotNull DateTimeFormatter dateTimeFormatter) {
        Log.dateTimeFormatter = dateTimeFormatter;
    }

    @Nullable
    public static Log.Filter getFilter() {
        return filter;
    }

    public static void setFilter(@Nullable Log.Filter filter) {
        Log.filter = filter;
    }

    static synchronized boolean startupShared(@NotNull LogFileResolver resolver) {
        // shutdown
        shutdownShared();

        logFileResolver = resolver;
        onLogFileChange = new EventDispatch<>("Log.onLogFileChange");
        onLogFileChange.addObserver(e -> {
            File file = e.arg;
            if (file == null) {
                file = logFileResolver.getCurrentLogFile();
            }
            switchSharedLogFile(file);
        });

        logFileResolver.onCreate(onLogFileChange);

        shared = new Log(logFileResolver.getCurrentLogFile(), logFileResolver.getLogZoneId());
        try {
            shared.startup();
            return true;
        } catch (IOException e) {
            System.err.println("failed to start up log: " + e);
            shared = null;
            logFileResolver = null;
            onLogFileChange = null;
            return false;
        }
    }

    static synchronized void shutdownShared() {
        if (shared != null) {
            shared.shutdown();
            shared = null;
            logFileResolver = null;
            onLogFileChange = null;
        }
    }

    private static synchronized void switchSharedLogFile(@NotNull File newFile) {
        if (shared != null && logFileResolver != null &&
                !shared.getLogFile().equals(newFile)) {
            shared.shutdown();
            shared = new Log(newFile, logFileResolver.getLogZoneId());
            try {
                shared.startup();
            } catch (IOException e) {
                System.err.println("failed to start up log: " + e);
                shared = null;
                logFileResolver = null;
                onLogFileChange = null;
            }
        }
    }

    @Nullable
    private static synchronized Log determineLogIns() {
        if (shared != null && logFileResolver != null) {
            logFileResolver.onLog(); // may cause switching log file!
            return shared;
        } else {
            return null;
        }
    }

    public static void i(@NotNull String tag, @NotNull String message) {
        Log log = determineLogIns();
        if (log != null) {
            log.info(tag, message);
        }
    }

    public static void i(@NotNull String tag, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.info(tag, ex);
        }
    }

    public static void i(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.info(tag, message, ex);
        }
    }

    public static void w(@NotNull String tag, @NotNull String message) {
        Log log = determineLogIns();
        if (log != null) {
            log.warning(tag, message);
        }
    }

    public static void w(@NotNull String tag, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.warning(tag, ex);
        }
    }

    public static void w(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.warning(tag, message, ex);
        }
    }

    public static void e(@NotNull String tag, @NotNull String message) {
        Log log = determineLogIns();
        if (log != null) {
            log.error(tag, message);
        }
    }

    public static void e(@NotNull String tag, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.error(tag, ex);
        }
    }

    public static void e(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        Log log = determineLogIns();
        if (log != null) {
            log.error(tag, message, ex);
        }
    }

    @NotNull private final File logFile;
    private boolean opened;
    @Nullable private FileOutputStream writer;
    @Nullable private ExecutorService executor;

    @NotNull private final ZoneId zoneId;
    @NotNull private final Charset charset;

    @NotNull private final List<Runnable> tasks;

    public Log(@NotNull File logFile, @NotNull ZoneId zoneId) {
        this.logFile = logFile;
        this.zoneId = zoneId;
        this.charset = StandardCharsets.UTF_8;
        this.tasks = Collections.synchronizedList(new ArrayList<>());
    }

    @NotNull
    public File getLogFile() {
        return logFile;
    }

    public synchronized boolean isOpened() {
        return opened;
    }

    public synchronized void startup() throws IOException {
        if (opened) {
            throw new IllegalStateException("the log file is already opened");
        }
        executor = Executors.newSingleThreadExecutor();
        try {
            writer = new FileOutputStream(logFile, true);
            opened = true;
        } catch (IOException e) {
            writer = null;
            throw e;
        }
    }

    public synchronized void shutdown() {
        if (!opened) {
            return;
        }
        assert executor != null;
        executor.shutdown();
        executor = null;
        for (Runnable t : tasks.toArray(new Runnable[0])) {
            t.run();
        }
        assert tasks.isEmpty();
        try {
            assert writer != null;
            writer.flush();
        } catch (IOException ignored) {
        } finally {
            try {
                assert writer != null;
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writer = null;

        opened = false;
    }

    private void atomWriteLines(char level, @NotNull String tag, @Nullable String lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        ExecutorService executor = this.executor;
        if (executor != null) {
            final Runnable task = new Runnable() {
                @Override
                public void run() {
                    synchronized (Log.this) {
                        if (writer != null) {

                            // generate timestamp
                            OffsetDateTime dateTime = OffsetDateTime.now(zoneId);
                            String time = dateTime.format(dateTimeFormatter);

                            try {
                                for (String line : lines.split("\n")) {
                                    if (line.isEmpty()) continue;

                                    if (filter != null) {
                                        if (!filter.accept(dateTime, level, tag, line)) {
                                            continue;
                                        }
                                    }

                                    // NOTE: Do not change format. See LogParser.
                                    String msg = String.format(Locale.US, "%s  %c  %s \t%s\n", time, level, tag, line);
                                    if (level == 'e') {
                                        System.err.print(msg);
                                    } else {
                                        System.out.print(msg);
                                    }
                                    writer.write(msg.getBytes(charset));
                                }
                                writer.flush();
                            } catch (IOException ignored) {
                            }

                        }
                        tasks.remove(this);
                    }
                }
            };
            tasks.add(task);
            executor.execute(task);
        } else {
            // already shutdown, ignore the log
        }
    }

    public void info(@NotNull String tag, @NotNull String message) {
        atomWriteLines('i', tag, message);
    }

    public void info(@NotNull String tag, @NotNull Exception ex) {
        atomWriteLines('i', tag, getExceptionMessage(ex));
    }

    public void info(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        atomWriteLines('i', tag, message);
        atomWriteLines('i', tag, getExceptionMessage(ex));
    }

    public void warning(@NotNull String tag, @NotNull String message) {
        atomWriteLines('w', tag, message);
    }

    public void warning(@NotNull String tag, @NotNull Exception ex) {
        atomWriteLines('w', tag, getExceptionMessage(ex));
    }

    public void warning(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        atomWriteLines('w', tag, message);
        atomWriteLines('w', tag, getExceptionMessage(ex));
    }

    public void error(@NotNull String tag, @NotNull String message) {
        atomWriteLines('e', tag, message);
    }

    public void error(@NotNull String tag, @NotNull Exception ex) {
        atomWriteLines('e', tag, getExceptionMessage(ex));
    }

    public void error(@NotNull String tag, @Nullable String message, @NotNull Exception ex) {
        atomWriteLines('e', tag, message);
        atomWriteLines('e', tag, getExceptionMessage(ex));
    }

    @Nullable
    private String getExceptionMessage(@NotNull Exception ex) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            PrintWriter writer = new PrintWriter(osw);
            ex.printStackTrace(writer);
            writer.flush();
            byte[] bytes = stream.toByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

}
