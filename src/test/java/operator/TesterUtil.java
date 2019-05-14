package operator;

import java.util.*;

class TesterUtil {
    static String renderWord(int size, Random rng) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append((char) (rng.nextInt(26) + 'a'));
        }
        return builder.toString();
    }

    static class DecodeRes {
        String str;
        long number;
        int cnt;

        DecodeRes(String str, long number, int cnt) {
            this.str = str;
            this.number = number;
            this.cnt = cnt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DecodeRes decodeRes = (DecodeRes) o;
            return number == decodeRes.number &&
                    cnt == decodeRes.cnt &&
                    decodeRes.str.equals(str);
        }

        @Override
        public int hashCode() {
            return Objects.hash(str, number, cnt);
        }
    }

    static DecodeRes decode(String str) {
        int ind = str.lastIndexOf("\t");
        int ind2 = str.lastIndexOf("\t", ind - 1);
        return new DecodeRes(str.substring(0, ind2),
                Long.parseLong(str.substring(ind2, ind).trim()),
                Integer.parseInt(str.substring(ind).trim()));
    }
}