package org.reprovados;

public class ProcessClock {

    private Long time;
    private Long clockIncrement;

    public ProcessClock(Long startTime, Long clockIncrement) {
        this.time = startTime;
        this.clockIncrement = clockIncrement;

        new Thread(() -> {
            while (true) {
                try {
                    //System.out.println("Hora Anterior: " + processClock.getTime());
                    Thread.sleep(1000);
                    this.incrementTime(this.clockIncrement);
                    // System.out.println("Hora Atual: " + processClock.getTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public long getTime() {
        return this.time;
    }

    public void incrementTime(long value) {
        this.time += value;
    }
}
