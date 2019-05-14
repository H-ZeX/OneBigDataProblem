package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Util {
    public static long availableMemory() {
        Runtime rt = Runtime.getRuntime();
        long t1 = rt.freeMemory();
        long t2 = rt.maxMemory() - rt.totalMemory();
        return t1 + t2;
    }

    /**
     * Only put non-empty str to buffer, If end, put empty str to the buffer.
     */
    public static void reader(BufferedReader reader, BlockingQueue<String> buffer) throws IOException, InterruptedException {
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == 0) {
                break;
            }
            buffer.put(line);
        }
        buffer.put("");
    }
}