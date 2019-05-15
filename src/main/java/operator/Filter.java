package operator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

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

    public static Result filter(String inputFile) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        // Map str to number, if number==null, this str is deprecated.
        HashMap<String, Long> map = new HashMap<>();
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
