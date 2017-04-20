package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Gong Zhang
 */
public final class App {

    private App() {}

    public static void main(@NotNull AppDelegate delegate) {
        Log.startupShared(delegate.getLogFileResolver());

        try {
            delegate.onStart();
        } catch (Exception ex) {
            System.err.println("Uncaught exception in App.onStart: " + ex.toString());
        }

        // register shutdown handler
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                delegate.onStop();
            } catch (Exception ex) {
                System.err.println("Uncaught exception in App.onStop: " + ex.toString());
            }
            Log.shutdownShared();
            ThreadPool.shutdown();
        }));

        // handle command line input
        Thread stdin_thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        delegate.onCommand(line);
                    } catch (Exception ex) {
                        System.err.println("Uncaught exception in App.onCommand: " + ex.toString());
                    }
                }
            } catch (IOException ignored) {
            }

            Runtime.getRuntime().exit(0);
        });

        stdin_thread.setName("App.stdin");
        stdin_thread.setDaemon(false);
        stdin_thread.start();
    }

}
