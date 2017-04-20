package co.gongzh.servicekit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.ZoneId;

import static org.junit.Assert.*;

/**
 * @author Gong Zhang
 */
public class LogTest {

    private static final String TAG = "LogTest";

    @Before
    public void setUp() throws Exception {
        Log.startupShared(new File("log.txt"), ZoneId.systemDefault());
    }

    @After
    public void tearDown() throws Exception {
        Log.shutdownShared();
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
