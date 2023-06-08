package org.reprovados;
import java.util.concurrent.atomic.AtomicLong;

public class GlobalTime {
    private static final AtomicLong time = new AtomicLong(1000);

    public static long getTime() {
        return time.get();
    }

    public static void incrementTime(long value) {
        time.addAndGet(value);
    }
}
