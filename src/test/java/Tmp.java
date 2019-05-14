import java.io.*;

public class Tmp {
    public static void main(String[] args) throws IOException {
        File file = new File("/tmp/split_tests/test_rf");
        System.out.println(file.length());
        FileOutputStream output = new FileOutputStream("/tmp/split_tests/test_rf");
        for (int i = 0; i < 10; i++) {
            output.write(i);
        }
        output.close();
        System.out.println(file.length());

        String name = "/tmp/rf__1";
        RandomAccessFile rf = new RandomAccessFile(name, "rw");
        rf.setLength(1024 * 1024);
        System.out.println(new File(name).length());
    }
}
