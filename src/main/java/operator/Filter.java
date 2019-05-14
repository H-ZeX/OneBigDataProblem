package operator;

import util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class Filter {
    public static class Result {
        public final String str;
        public final long number;
        public final boolean valid;

        /**
         * @param str If str is null, then this result is invalid.
         */
        public Result(String str, long number) {
            this.str = str;
            this.number = number;
            this.valid = str != null;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "str='" + str + '\'' +
                    ", number=" + number +
                    ", valid=" + valid +
                    '}';
        }
    }

    public static Result filter(String inputFile, int bufferSize)
            throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        ArrayBlockingQueue<String> buffer = new ArrayBlockingQueue<>(bufferSize);
        new Thread(() -> {
            try {
                Util.reader(reader, buffer);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        // Map str to number, if number==null, this str is deprecated.
        HashMap<String, Long> map = new HashMap<>();
        while (true) {
            String line = buffer.take();
            if (line.length() == 0 || line.charAt(0) == 0) {
                break;
            }

            int ind = line.lastIndexOf("\t");
            int ind2 = line.lastIndexOf("\t", ind - 1);
            String str = line.substring(0, ind2);
            long num = Long.parseLong(line.substring(ind2, ind).trim());
            int cnt = Integer.parseInt(line.substring(ind).trim());

            if (cnt > 1 || map.containsKey(line)) {
                map.put(line, null);
            } else {
                map.put(str, num);
            }
        }
        String rs = null;
        long rn = Long.MAX_VALUE;
        for (String x : map.keySet()) {
            Long v = map.get(x);
            if (v != null && v < rn) {
                rs = x;
                rn = v;
            }
        }
        return new Result(rs, rn);
    }
}
