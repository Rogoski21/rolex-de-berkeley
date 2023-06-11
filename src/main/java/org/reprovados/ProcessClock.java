package org.reprovados;

public class ProcessClock {

    private Long time = 0L;

    public ProcessClock(Long startTime) {
        this.time = startTime;
    }

    public long getTime() {
        return this.time;
    }

    public void incrementTime(long value) {
        this.time += value;
    }
}
