package org.reprovados;
import java.util.concurrent.atomic.AtomicLong;

public class GlobalTime {
    private static Long time = 1000l;

    public static long getTime() {
        return time;
    }

    public static void incrementTime(long value) {
        time = value;
    }
}
