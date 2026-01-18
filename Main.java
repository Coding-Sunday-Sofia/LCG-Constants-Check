/*
clear && javac Main.java && java Main
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class Main {
    private static final Random PRNG = new SecureRandom();

    private static long a;
    private static long aMin;
    private static long aMax;

    private static long c;
    private static long cMin;
    private static long cMax;

    private static long m;
    private static long mMin;
    private static long mMax;

    private static long sampleSize;

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties properties = new Properties();
        InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties");
        properties.load(input);

        aMin = Long.parseLong(properties.getProperty("a.min", ""+1));
        aMax = Long.parseLong(properties.getProperty("a.max", ""+((1L<<32)-1)));

        cMin = Long.parseLong(properties.getProperty("c.min", ""+0));
        cMax = Long.parseLong(properties.getProperty("c.max", ""+((1L<<32)-1)));

        mMin = Long.parseLong(properties.getProperty("m.min", ""+(1L<<31)));
        mMax = Long.parseLong(properties.getProperty("m.max", ""+(1L<<32)));

        sampleSize = Long.parseLong(properties.getProperty("sample.size", ""+1048576));

        Set<String> tracking = new HashSet<>(Files.readAllLines(Paths.get("tracking.txt")));

        String ticker = "";
        do {
            a = aMin + ((Math.abs(PRNG.nextLong()) % (aMax - aMin + 1)));
            c = cMin + ((Math.abs(PRNG.nextLong()) % (cMax - cMin + 1)));
            m = mMin + ((Math.abs(PRNG.nextLong()) % (mMax - mMin + 1)));
            ticker = String.format("%012d", a) + String.format("%012d", c) + String.format("%012d", m);
        } while (tracking.contains(ticker));
        byte[] values = new byte[(int)sampleSize*4];

        while (true) {
            long x = 1;
            for (int i = 0; i < values.length; i+=4) {
                x = (a * x + c) % m;
                //TODO Is it little-endian?
                values[i] = (byte)(x & 0xFF);
                values[i+1] = (byte)((x>>8) & 0xFF);
                values[i+2] = (byte)((x>>16) & 0xFF);
                values[i+3] = (byte)((x>>24) & 0xFF);
            }

            /* Write generated numbers in a file. */
            FileOutputStream output = (new FileOutputStream(ticker+".bin"));
            output.write(values);
            output.close();

            /* Runing Dieharder bundle. */
            ((new ProcessBuilder("dieharder", "-a", "-g", "201", "-f", ""+ticker+".bin").redirectOutput(new File(ticker+".log"))).start()).waitFor();

            /* Delete binary sample file. */
            (new File(ticker+".bin")).delete();

            /* Keep trakcing of the checked constants. */
            tracking.add(ticker);
            Files.write(Paths.get("tracking.txt"), tracking);
        }
    }
}
