package main;

import product.FindTarget;

import java.io.File;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws Exception {
        final Param param = checkAndParseParam(args);
        final String splitBaseName = "split_1";
        final FindTarget tf = new FindTarget(param.middleFileSize, param.freeMemLowBound);
        System.out.println(tf.work(splitBaseName, param.inputFile, param.middleDir));
    }

    private static class Param {
        private String inputFile;
        private String middleDir;
        private long middleFileSize;
        private long freeMemLowBound;

        Param(String inputFile, String middleDir, long middleFileSize, long freeMemLowBound) {
            this.inputFile = inputFile;
            this.middleDir = middleDir;
            this.middleFileSize = middleFileSize;
            this.freeMemLowBound = freeMemLowBound;
        }
    }

    private static Param checkAndParseParam(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("usage: ftw <inputFileName> <middleDir> " +
                    "[middleFileSize(in percentage)] [freeMemLowBound(in percentage)]" +
                    "\nIf the middleFileSize and freeMemLowBound is not legal, " +
                    "they wil be set to default value" +
                    "\nMake sure that the middleDir is empty, or it is not exist");
            System.exit(0);
        }
        final String inputFile = args[0];
        final String middleDir = args[1];
        final long maxMem = Runtime.getRuntime().maxMemory();
        long middleFileSize = -1;
        long freeMemLowBound = -1;
        if (args.length >= 3) {
            middleFileSize = (long) (Double.parseDouble(args[2]) * maxMem);
        }
        if (args.length >= 4) {
            freeMemLowBound = (long) (Double.parseDouble(args[3]) * maxMem);
        }
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
                System.err.println("The middleDir should be empty");
                System.exit(1);
            }
        }
        return new Param(inputFile, middleDir, middleFileSize, freeMemLowBound);
    }
}
