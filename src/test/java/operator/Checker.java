package operator;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Checker {
    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        File[] files = dir.listFiles();
        assert files != null;
        ArrayList<SN> list = new ArrayList<>();
        for (File x : files) {
            list.addAll(readProcessedFile(x.getAbsolutePath()));
        }

        ArrayList<SN> nm = cleanMaterial(readMaterial(args[1]));

        nm.sort(Comparator.comparingLong(x -> x.number));
        list.sort(Comparator.comparing(x -> x.number));
        assert nm.equals(list) : "\n" + nm + "\n" + list;
    }

    private static ArrayList<SN> readMaterial(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ArrayList<SN> ans = new ArrayList<>();
        long number = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null || line.charAt(0) == 0) {
                break;
            }
            ans.add(new SN(line, number++));
        }
        return ans;
    }

    private static ArrayList<SN> readProcessedFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        HashMap<String, SN> map = new HashMap<>();
        while (true) {
            String line = reader.readLine();
            if (line == null || line.charAt(0) == 0) {
                break;
            }
            TesterUtil.DecodeRes d;
            try {
                d = TesterUtil.decode(line);
            } catch (Throwable throwable) {
                System.out.println(line);
                throw throwable;
            }
            if (map.containsKey(d.str) || d.cnt > 1) {
                map.put(d.str, null);
            } else {
                assert d.cnt == 1 : d.cnt;
                map.put(d.str, new SN(d.str, d.number, 1));
            }
        }

        ArrayList<SN> ans = new ArrayList<>();
        for (String x : map.keySet()) {
            assert map.get(x) == null || map.get(x).str.equals(x) : map.get(x) + ", " + x;
            if (map.get(x) != null) {
                ans.add(map.get(x));
            }
        }
        return ans;
    }

    private static class SN {
        String str;
        long number;
        int cnt;

        SN(String str, long number, int cnt) {
            this.str = str;
            this.number = number;
            this.cnt = cnt;
        }

        @Override
        public String toString() {
            return "SN{" +
                    "str='" + str + '\'' +
                    ", number=" + number +
                    ", cnt=" + cnt +
                    '}';
        }

        SN(String str, long number) {
            this.str = str;
            this.number = number;
            this.cnt = 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SN sn = (SN) obj;
            return number == sn.number &&
                    cnt == sn.cnt &&
                    str.equals(sn.str);
        }
    }

    private static ArrayList<SN> cleanMaterial(ArrayList<SN> list) {
        HashMap<String, SN> map = new HashMap<>();
        for (SN x : list) {
            x.cnt = map.containsKey(x.str) ? 2 : 1;
            map.put(x.str, x);
        }
        ArrayList<SN> ans = new ArrayList<>();
        for (String x : map.keySet()) {
            if (map.get(x).cnt == 1) {
                ans.add(map.get(x));
            }
        }
        return ans;
    }
}
