package org.reprovados;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Berkeley {

    private static final String MULTICAST_ADDRESS = "239.0.0.1";
    private static final int MULTICAST_PORT = 8888;
    private static final int MASTER_PORT = 8000;

    private static final long MAX_TOLERATION = 10000;

    private static BlockingQueue<String> messagesList = new LinkedBlockingQueue<>();
    private static final List<Process> initialAverageProcesses = new ArrayList<>();
    private static final List<Process> finalAverageProcesses = new ArrayList<>();

    // process clock
    private static ProcessClock processClock;

    // program params
    private static int processId;
    private static String processHost;
    private static int processPort;
    private static long processTime;
    private static long sendTime;
    private static long aDelay;

    public static void main(String[] args) {
        processId = Integer.parseInt(args[0]);
        processHost = args[1];
        processPort = Integer.parseInt(args[2]);
        processTime = Long.parseLong(args[3]);
        aDelay = Long.parseLong(args[4]);

        // Getting local ip address; Not localhost
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            processHost = socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }

        String address = processHost + ":" + processPort;

        processClock = new ProcessClock(Long.parseLong(args[5]), Long.parseLong(args[6]));

        if (args.length != 7) {
            System.out.println("The program should run with 6 arguments. i.e: java Process <processId> <host> <port> <time> <ptime> <adelay>");
        }

        if (processId == 0) {
            System.out.println("[MASTER] Initialized at: " + address);
            bossProcess(processHost, processPort);
        } else {
            System.out.println("[SLAVE] Initialized at: " + address);
            slaveProcess();
        }

    }

    private static void bossProcess(String host, int port) {
        try {
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);

            multicastSocket.joinGroup(multicastAddress);

            // recebe as horas do estag
            DatagramSocket unicastSocket = new DatagramSocket(port);
            // Fluxo principal

            while (true) {
                String message = "[MASTER] Sending multicast message to get slaves time.";
                System.out.println(message);

                // envia mensagem pedindo o tempo dos escravos em multicast
                byte[] buffer = message.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, multicastAddress, MULTICAST_PORT);
                multicastSocket.send(datagramPacket);
                sendTime = processClock.getTime();

                // Adiciona sua própria hora na lista
                System.out.println("[MASTER] Adding self time to process list, master time: " + processClock.getTime());
                initialAverageProcesses.add(new Process(0, processHost, processPort, processClock.getTime(), 0, 0));

                // recebe a hora dos escravos
                while (initialAverageProcesses.size() < 3 /* número de processos incluindo o master*/) {
                    byte[] abuffer = new byte[256];
                    DatagramPacket slavePacket = new DatagramPacket(abuffer, abuffer.length);
                    //System.out.println("Aguardando mensagem");

                    Thread.sleep(10);
                    // Tempo para o mestre poder inicilizar a espera de uma comunicação unicast (usaremos como descompasso do mestre)
                    unicastSocket.receive(slavePacket);
                    long receiveTime = processClock.getTime();

                    //System.out.println("mensagem recebida");

                    String[] receivedMessage = new String(slavePacket.getData(), 0, slavePacket.getLength()).split(";");
                    System.out.println("[MASTER] Received a message: "+ Arrays.toString(receivedMessage));

                    // "id;comando;tempo;aDelay"
                    int id = Integer.parseInt(receivedMessage[0]);
                    String command = receivedMessage[1];

                    if (!command.equals("mytime")) {
                        continue;
                    }

                    long time = Long.parseLong(receivedMessage[2]);
                    long adelay = Long.parseLong(receivedMessage[3]);
                    long rtt = (receiveTime - sendTime) + adelay; // + adelay de cada processo



                    var process = new Process(id, slavePacket.getAddress().getHostAddress(), slavePacket.getPort(), time, adelay, rtt);

                    initialAverageProcesses.add(process);
                    System.out.println("SLAVE " + process.getId() + " with time " + process.getCurrentTime());
                }

                boolean repeatAverageCalc = true;

                finalAverageProcesses.addAll(initialAverageProcesses);
                //Verifica se precisa calcular uma nova média

                long average = 0;
                while (repeatAverageCalc) {

                    finalAverageProcesses.forEach(x -> {
                        System.out.println("Process Id: " + x.getId() +" Proccess Time: "+ x.getCurrentTime());
                    });

                    // Se todos forem destoantes, pega o processo do mestre
                    if (finalAverageProcesses.isEmpty()) {
                        System.out.println("Todos os processos estão com o tempo discrepante, nova média utilizando o tempo do mestre");
                        average = processClock.getTime();
                        System.out.println("Média calculada: " + average);
                        break;
                    }

                    long somaTudo = 0;

                    //Faz a media da lista dos escravos
                    for (Process p : finalAverageProcesses) {
                        somaTudo += p.getCurrentTime();
                    }

                    average = somaTudo / finalAverageProcesses.size();
                    System.out.println("Média calculada: " + average);

                    int includedSize = finalAverageProcesses.size();

                    long finalAverage = average;

                    System.out.println("Processos utilizados: " + finalAverageProcesses);

                    finalAverageProcesses.removeIf(p -> Math.abs(p.getCurrentTime() - finalAverage) > MAX_TOLERATION);

                    repeatAverageCalc = includedSize != finalAverageProcesses.size();

                    if (repeatAverageCalc) {
                        System.out.println("Calculando nova média");
                    }
                }

                for (Process process : initialAverageProcesses) {
                    var timeToAdjust = average - process.getCurrentTime();

                    if (process.getId() == 0) {
                        processClock.incrementTime(timeToAdjust);
                    } else {
                        long oneDelayWay = process.getRtt() / 2;
                        long finalTime = timeToAdjust + oneDelayWay;
                        System.out.println("Tempo de ajuste em ms:" + timeToAdjust + " One Way Delay:" + oneDelayWay + " Ajuste Final:" +finalTime );
                        var timeAdjustMessage = 0 + ";adjusttime;" + timeToAdjust;

                        byte[] timeAdjustBuffer = timeAdjustMessage.getBytes();

                        var packet = new DatagramPacket(timeAdjustBuffer, timeAdjustBuffer.length,
                                InetAddress.getByName(process.getHost()), process.getPort());

                        unicastSocket.send(packet);
                    }
                }

                Thread.sleep(5000); // 30 segundos na versão final

                System.out.println("-----------------------");

                initialAverageProcesses.clear();
                finalAverageProcesses.clear();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void slaveProcess() {
        try {
            DatagramSocket unicastSocket = new DatagramSocket(processPort);
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] abuffer = new byte[256];
                        DatagramPacket slavePacket = new DatagramPacket(abuffer, abuffer.length);
                        System.out.println("[SLAVE " + processId + "] waiting for master request.");
                        unicastSocket.receive(slavePacket);

                        String receivedMessage = new String(slavePacket.getData(), 0, slavePacket.getLength());
                        messagesList.add(receivedMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    sendTime(unicastSocket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Thread que ajusta o próprio tempo
            new Thread(() -> {
                try {
                    while (true) {
                        if (messagesList.isEmpty()) {
                            continue;
                        }
                        String message = messagesList.peek();
                        String[] messageInfo = message.split(";");

                        String command = messageInfo[1];
                        long time = Long.parseLong(messageInfo[2]);
                        if (command.equals("adjusttime")) {
                            System.out.println("Tempo antes de ajustar: " + processClock.getTime());
                            System.out.println("Ajuste em ms: " + time);
                            processClock.incrementTime(time);
                            System.out.println("Tempo ajustado: " + processClock.getTime());
                            messagesList.poll();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendTime(DatagramSocket unicastSocket) throws IOException {
        InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastSocket.joinGroup(multicastAddress);

        while (true) {
            byte[] buffer = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            multicastSocket.receive(datagramPacket);
            System.out.println("[SLAVE " + processId + "] Tempo solicitado pelo mestre no instante: " + processClock.getTime());

            System.out.println("[SLAVE " + processId + "] Incrementando tempo com o processTime de: " + processTime);
            processClock.incrementTime(processTime);

            // Processa a mensagem recebida do mestre e gera a resposta
            System.out.println("[SLAVE "  + processId + "] Tempo enviado para o mestre (incluindo process time):" + processClock.getTime());

            // "id;comando;tempo; aDelay"
            String answer = processId + ";mytime;" + processClock.getTime() + ";" + aDelay;

            byte[] answerBuffer = answer.getBytes();

            DatagramPacket answerPacket = new DatagramPacket(answerBuffer, answerBuffer.length,
                    datagramPacket.getAddress(), MASTER_PORT);

            unicastSocket.send(answerPacket);

            System.out.println("---------------------------------");
        }
    }
}
