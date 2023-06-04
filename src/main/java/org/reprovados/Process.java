package org.reprovados;

import java.util.StringJoiner;

public class Process {
    private String host;
    private int port;
    private long currentTime;
    private int aDelay;

    public Process(String host, int port, long currentTime) {
        this.host = host;
        this.port = port;
        this.currentTime = currentTime;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", Process.class.getSimpleName() + "[", "]")
                .add("host='" + host + "'")
                .add("port=" + port)
                .add("currentTime=" + currentTime)
                .add("aDelay=" + aDelay)
                .toString();
    }
}
