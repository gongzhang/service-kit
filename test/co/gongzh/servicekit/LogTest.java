package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * @author Gong Zhang
 */
public class LogTest {

    private static final String TAG = "LogTest";

    @BeforeClass
    public static void setUp() throws Exception {
        Log.startupShared(new LogFileResolver() {

            EventDispatch<File> onLogFileChange;

            int fileNum = 0;

            @Override
            public void onCreate(@NotNull EventDispatch<File> onLogFileChange) {
                this.onLogFileChange = onLogFileChange;
            }

            @Override
            public void onLog() {
                if (fileNum > 0) {
                    onLogFileChange.fire(getCurrentLogFile());
                }
                fileNum += 1;
            }

            @Override
            public @NotNull File getCurrentLogFile() {
                return new File("log" + fileNum + ".txt");
            }

        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.shutdownShared();
        boolean ignored;
        ignored = new File("log0.txt").delete();
        ignored = new File("log1.txt").delete();
        ignored = new File("log2.txt").delete();
    }

    @Test
    public void i() throws Exception {
        Log.i(TAG, "This is an information.");
    }

    @Test
    public void w() throws Exception {
        Log.w(TAG, "This is a warning.");
    }

    @Test
    public void e() throws Exception {
        try {
            int ignored = Integer.parseInt("not a int");
        } catch (NumberFormatException ex) {
            Log.e(TAG, "This is an error with exception.", ex);
        }
    }

}
