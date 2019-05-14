package operator;

import util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

public class Split {
    public static class LineParseResult {
        private boolean ok;
        private String str;
        private long number;

        public LineParseResult(boolean ok, String str, long number) {
            this.ok = ok;
            this.str = str;
            this.number = number;
        }
    }

    private final long RESERVED_MEM_SIZE;

    /*
     * Map str to number, if number ==-1, then this str is deprecated.
     */
    private HashMap<String, Long>[] containers;
    private FileOutputStream[] outputs;
    private BufferedReader reader;
    private Function<String, Integer> hashFunc;
    private BiFunction<String, Long, LineParseResult> lineParser;
    private ArrayBlockingQueue<String> buffer;

    /**
     * Split the inputFile to outputDirs, one dir one file.
     * If two string has same result of hashFunc, then they are split to the same dir.
     * The fileName of file output to one outputDir is prefixed with this Split's name.
     *
     * @param name       The name of this split, it should be unique among all split.
     * @param outputDirs The len of outputDirs should equal to partCnt
     */
    @SuppressWarnings("unchecked")
    public Split(String name, String inputFileName,
                 String[] outputDirs,
                 Function<String, Integer> hashFunc,
                 BiFunction<String, Long, LineParseResult> lineParser,
                 long reverseMemSize,
                 int bufferSize)
            throws Exception {
        if (outputDirs.length == 0) {
            throw new IllegalArgumentException("The outputDirs should not be empty");
        }
        final int partCnt = outputDirs.length;
        final long inputFileSize = new File(inputFileName).length();

        this.RESERVED_MEM_SIZE = reverseMemSize;
        this.outputs = new FileOutputStream[partCnt];
        this.containers = new HashMap[partCnt];
        this.reader = new BufferedReader(new FileReader(inputFileName));
        this.hashFunc = (x) -> (hashFunc.apply(x) % partCnt + partCnt) % partCnt;
        this.lineParser = lineParser;
        this.buffer = new ArrayBlockingQueue<>(bufferSize);

        for (int i = 0; i < partCnt; i++) {
            File dir = new File(outputDirs[i]);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Can not create dir: " + outputDirs[i]);
            }
            String fn = outputDirs[i] + "/" + name + "_" + i;
            new File(fn).delete();

            RandomAccessFile rf = new RandomAccessFile(fn, "rw");
            rf.setLength(inputFileSize);
            rf.close();
            outputs[i] = new FileOutputStream(fn);
            containers[i] = new HashMap<>();
        }
    }

    public void start() throws IOException, InterruptedException {
        new Thread(() -> {
            try {
                Util.reader(this.reader, this.buffer);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        work();
    }

    private void work() throws IOException, InterruptedException {
        long num = -1;
        while (true) {
            String line = buffer.take();
            if (line.length() == 0) {
                break;
            }
            num++;
            LineParseResult r = lineParser.apply(line, num);
            if (!r.ok) {
                continue;
            }
            int hash = this.hashFunc.apply(r.str);
            HashMap<String, Long> m = containers[hash];
            if (m.containsKey(r.str)) {
                m.put(r.str, -1L);
            } else {
                m.put(r.str, r.number);
            }
            if (Util.availableMemory() < RESERVED_MEM_SIZE) {
                flushToDisk();
            }
        }
        flushToDisk();
        for (FileOutputStream rf : outputs) {
            rf.write(0);
            rf.write("\n".getBytes());
        }
        destructor();
    }

    private void flushToDisk() throws IOException {
        Logger.getGlobal().info("Flush to disk");
        for (int i = 0; i < containers.length; i++) {
            for (String x : containers[i].keySet()) {
                Long value = containers[i].get(x);
                outputs[i].write(x.getBytes());
                outputs[i].write("\t".getBytes());
                outputs[i].write(value.toString().getBytes());
                outputs[i].write("\t".getBytes());
                outputs[i].write(value != -1 ? "1".getBytes() : "2".getBytes());
                outputs[i].write("\n".getBytes());
            }
            containers[i] = new HashMap<>();
            // TODO: should I flush?
        }
        System.gc();
    }

    private void destructor() throws IOException {
        for (FileOutputStream x : outputs) {
            x.close();
        }
        for (int i = 0; i < containers.length; i++) {
            containers[i] = null;
        }
        this.buffer = null;
        this.hashFunc = null;
        this.outputs = null;
        this.reader = null;
        System.gc();
    }
}
