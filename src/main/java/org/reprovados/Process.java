package org.reprovados;

import java.util.StringJoiner;

public class Process {
    private int id;
    private String host;
    private int port;
    private long currentTime;
    private int aDelay;

    public Process(int id, String host, int port, long currentTime) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.currentTime = currentTime;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public int getaDelay() {
        return aDelay;
    }

    public void setaDelay(int aDelay) {
        this.aDelay = aDelay;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
