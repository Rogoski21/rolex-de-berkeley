package org.reprovados;

import java.util.StringJoiner;

public class Process {
    private int id;
    private String host;
    private int port;
    private long currentTime;
    private long aDelay;
    private long rtt;

    public Process(int id, String host, int port, long currentTime, long aDelay, long rtt) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.currentTime = currentTime;
        this.aDelay = aDelay;
        this.rtt = rtt;
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

    public long getaDelay() {
        return aDelay;
    }

    public long getRtt() {
        return rtt;
    }

    public void setRtt(long rtt) {
        this.rtt = rtt;
    }

    public void setaDelay(long aDelay) {
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
                .add("id=" + id)
                .add("host='" + host + "'")
                .add("port=" + port)
                .add("currentTime=" + currentTime)
                .add("aDelay=" + aDelay)
                .add("rtt=" + rtt)
                .toString();
    }
}
