package co.gongzh.servicekit;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Gong Zhang
 */
public class LogParserTest {

    @Test
    public void parseLine() throws Exception {
        LogParser.Line line = LogParser.parseLine("2017-04-20T16:28:06.492+08:00  e  LED \tLauncher process seems dead.");
        assertNotNull(line);
        assertEquals("2017-04-20T16:28:06.492+08:00", line.timestamp);
        assertEquals('e', line.level);
        assertEquals("LED", line.tag);
        assertEquals("Launcher process seems dead.", line.message);
    }

    @Test
    public void parse() throws Exception {
        String log = "2017-04-20T16:28:06.492+08:00  e  LED \tLauncher process seems dead.\n" +
                     "2017-04-20T16:28:06.498+08:00  i  Cleaner \tNo old data to clear.\n" +
                     "\n" +
                     "bad line\n" +
                     "2017-04-20T16:28:06.724+08:00  w  SocketIO \tStatus approved. Start working...\n";
        List<LogParser.Line> lines = LogParser.parse(log);
        assertEquals(3, lines.size());
        assertEquals('e', lines.get(0).level);
        assertEquals('i', lines.get(1).level);
        assertEquals('w', lines.get(2).level);
    }

}
