package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class RenderFile {
    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.out.println("usage: render <wordCnt> <minWordSize> <maxWordSize> <outputFileName> <theOnlyStr>");
            System.exit(1);
        }
        int wordCnt = Integer.parseInt(args[0]);
        int minWordSize = Integer.parseInt(args[1]);
        int maxWordSize = Integer.parseInt(args[2]);
        if (wordCnt < 0 || minWordSize < 0 || maxWordSize < 0) {
            System.err.println("wordCnt, minWordSize, maxWordSize must >= 0");
            System.exit(1);
        }
        if (minWordSize > maxWordSize) {
            System.err.println("minWordSize should >= maxWordSize");
            System.exit(1);
        }
        render(wordCnt, minWordSize, maxWordSize, args[3], args[4]);
    }

    private static void render(int wordCnt, int minWordSize, int maxWorSize, String fileName, String result)
            throws IOException {
        final FileOutputStream output = new FileOutputStream(fileName);
        final Random rng = new Random();
        boolean hasRes = false;

        for (int i = 0; i < wordCnt; i++) {
            int size = rng.nextInt(maxWorSize - minWordSize) + minWordSize;
            String s;
            do {
                s = renderWord(size, rng);
            } while (s.equals(result));

            output.write((s + "\n").getBytes());
            for (int j = 0; j < rng.nextInt(2) + 2; j++) {
                output.write((s + "\n").getBytes());
            }
            if (!hasRes && rng.nextInt(10) == 1) {
                output.write((result + "\n").getBytes());
                hasRes = true;
            }
        }
        if (!hasRes) {
            output.write((result + "\n").getBytes());
        }
        output.close();
    }

    private static String renderWord(int size, Random rng) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append((char) (rng.nextInt(26) + 'a'));
        }
        return builder.toString();
    }
}
