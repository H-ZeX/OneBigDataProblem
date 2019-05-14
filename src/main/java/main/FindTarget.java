package main;

import operator.Filter;
import operator.Split;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

public class FindTarget {
    private final static long ALL_MEM = Runtime.getRuntime().maxMemory();
    private final static long FILE_SIZE_TO_FILTER = (long) (Math.ceil(ALL_MEM * 0.6));
    private final static long FREE_MEM_LOW_BOUND = (long) (ALL_MEM * 0.15);
    private final static int MIN_BUF_CAPACITY = 1024;
    private final static int BUF_CAPACITY;
    // The cnt of split round, if over this number,
    // exit and report that this program is not competent.
    private final static int MAX_SPLIT_ROUND_CNT = 5;
    @SuppressWarnings("unchecked")
    private final static Function<String, Integer>[] HASH_FUNC_SET =
            (Function<String, Integer>[]) new Function[MAX_SPLIT_ROUND_CNT];

    static {
        // Assume that every string's avg len is 100, then its size is 200.
        int tmp = (int) (ALL_MEM * 0.05) / 200;
        BUF_CAPACITY = tmp < MIN_BUF_CAPACITY ? MIN_BUF_CAPACITY : tmp;

        HASH_FUNC_SET[0] = String::hashCode;
        HASH_FUNC_SET[1] = (x) -> {
            if (x.length() <= 1) {
                return x.length() == 0 ? 0 : (int) x.charAt(0);
            } else {
                int l1 = x.length() / 2;
                return x.substring(0, l1).hashCode()
                        + x.substring(l1).hashCode();
            }
        };
        HASH_FUNC_SET[2] = (x) -> {
            if (x.length() <= 2) {
                int hash = 0;
                for (int i = 0; i < x.length(); i++) {
                    hash += x.charAt(i);
                }
                return hash;
            } else {
                int l1 = x.length() / 3;
                return x.substring(0, l1).hashCode()
                        + x.substring(l1 + l1 * 2).hashCode()
                        + x.substring(l1 * 2).hashCode();
            }
        };
        HASH_FUNC_SET[3] = (x) -> {
            if (x.length() <= 3) {
                int hash = 0;
                for (int i = 0; i < x.length(); i++) {
                    hash += x.charAt(i);
                }
                return hash;
            } else {
                int l1 = x.length() / 4;
                return x.substring(0, l1).hashCode()
                        + x.substring(l1 + l1 * 2).hashCode()
                        + x.substring(l1 * 2, l1 * 3).hashCode()
                        + x.substring(l1 * 3).hashCode();
            }
        };
        HASH_FUNC_SET[4] = (x) -> {
            if (x.length() <= 4) {
                int hash = 0;
                for (int i = 0; i < x.length(); i++) {
                    hash += x.charAt(i);
                }
                return hash;
            } else {
                int l1 = x.length() / 5;
                return x.substring(0, l1).hashCode()
                        + x.substring(l1 + l1 * 2).hashCode()
                        + x.substring(l1 * 2, l1 * 3).hashCode()
                        + x.substring(l1 * 3, l1 * 4).hashCode()
                        + x.substring(l1 * 4).hashCode();
            }
        };
    }

    public static void main(String[] args) throws Exception {
        final Param param = checkAndParseParam(args);
        final String splitBaseName = "split_1";

        split(splitBaseName, param.inputFile,
                param.middleDir, param.partCnt,
                HASH_FUNC_SET[0],
                (line, number) -> new Split.LineParseResult(true, line, number)
        );
        recurseSplit(splitBaseName, param.middleDir);
        filterRes(param.middleDir);
    }

    private static void recurseSplit(String splitBaseName,
                                     String middleDir)
            throws Exception {

        // Be careful that, the new file gene by split will in the same dir,
        // so it MUST not has same name as others.
        // So the split name should be construct carefully.
        boolean ok;
        int roundCnt = 0;
        do {
            ok = true;
            roundCnt++;
            if (roundCnt >= MAX_SPLIT_ROUND_CNT) {
                throw new Exception("This program is not competent for this job.");
            }
            File[] files = new File(middleDir).listFiles();
            assert files != null;
            for (int i = 0; i < files.length; i++) {
                File x = files[i];
                if (x.length() <= FILE_SIZE_TO_FILTER) {
                    continue;
                }
                Logger.getGlobal().info("recurse split, fileSize: " + x.length()
                        + ", fileName: " + x.getAbsolutePath()
                        + ", file_size_to_filter: " + FILE_SIZE_TO_FILTER);
                ok = false;
                int partCnt = (int) Math.ceil(x.length() * 1.0 / FILE_SIZE_TO_FILTER);
                split(splitBaseName + "_" + roundCnt + "_" + i,
                        x.getAbsolutePath(),
                        middleDir, partCnt,
                        HASH_FUNC_SET[roundCnt],
                        (line, num) -> {
                            int ind = line.lastIndexOf("\t");
                            int ind2 = line.lastIndexOf("\t", ind - 1);
                            String str = line.substring(0, ind2);
                            long n = Long.parseLong(line.substring(ind2, ind).trim());
                            int cnt = Integer.parseInt(line.substring(ind).trim());
                            return new Split.LineParseResult(cnt == 1, str, n);
                        }
                );
                Logger.getGlobal().info("delete " + x);
                if (!x.delete()) {
                    throw new Exception("delete middle file(" + x.getAbsolutePath() + ") failed");
                }
            }
        } while (!ok);
    }

    private static void split(String splitName,
                              String inputFileName,
                              String outputDir,
                              int partCnt,
                              Function<String, Integer> hashFunc,
                              BiFunction<String, Long, Split.LineParseResult> lineParser)
            throws Exception {
        String[] outputDirs = new String[partCnt];
        for (int i = 0; i < partCnt; i++) {
            outputDirs[i] = outputDir;
        }
        final Split split = new Split(splitName, inputFileName, outputDirs,
                hashFunc, lineParser, FREE_MEM_LOW_BOUND, BUF_CAPACITY);
        split.start();
    }

    private static void filterRes(String outputDir)
            throws IOException, InterruptedException {
        final ArrayList<Filter.Result> res = new ArrayList<>();
        final File[] files = new File(outputDir).listFiles();
        assert files != null;
        for (int i = 0; i < files.length; i++) {
            File p = files[i];
            res.add(Filter.filter(p.getAbsolutePath(), BUF_CAPACITY));
            Logger.getGlobal().info("End " + i + "'s filter");
        }
        String rs = null;
        long rn = Long.MAX_VALUE;
        for (Filter.Result x : res) {
            if (x.valid && rn > x.number) {
                rs = x.str;
                rn = x.number;
            }
        }
        System.out.println(rs == null ? "Not found" : rs);
    }

    private static class Param {
        private int partCnt;
        private String inputFile;
        private String middleDir;

        Param(int partCnt, String inputFile, String middleDir) {
            this.partCnt = partCnt;
            this.inputFile = inputFile;
            this.middleDir = middleDir;
        }
    }

    private static Param checkAndParseParam(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: ftw <inputFileName> <middleDir>" +
                    "\nMake sure that the middleDir is empty, or it is not exist");
            System.exit(0);
        }
        final String inputFile = args[0];
        final String middleDir = args[1];

        if (!new File(inputFile).exists()) {
            System.err.println(inputFile + " does not exist");
            System.exit(1);
        }
        final File dir = new File(middleDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("Can not create middleDir");
                System.exit(1);
            }
        } else {
            if (!dir.isDirectory()) {
                System.err.println("The middleDir is not dir");
                System.exit(1);
            } else if (Objects.requireNonNull(dir.listFiles()).length != 0) {
                System.err.println("The middleDir should not has other files");
                System.exit(1);
            }
        }
        long len = new File(inputFile).length();
        int partCnt = (int) (Math.ceil(len * 1.0 / FILE_SIZE_TO_FILTER) + 1);
        partCnt = partCnt < 10 ? 10 : partCnt;
        Logger.getGlobal().info("init, split to " + partCnt + " parts");
        return new Param(partCnt, inputFile, middleDir);
    }
}
