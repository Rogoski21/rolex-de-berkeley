package org.reprovados;

import javax.swing.tree.ExpandVetoException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Berkeley {

    private static final int MULTICAST_PORT = 8888;
    private static final int MASTER_PORT = 8000;
    private static final String MULTICAST_ADDRESS = "239.0.0.1";

    private static final long MAX_TOLERATION = 5000;

    private static final List<Process> included = new ArrayList<>();

    private static final List<Process> processes = new ArrayList<>();

    private static ProcessClock processClock;

    private static int processId;

    private static int processPort;

    public static void main(String[] args) {
        processId = Integer.parseInt(args[0]);
        final String host = args[1];
        processPort = Integer.parseInt(args[2]);
        final long processTime = Long.parseLong(args[3]);

        final long startTime = Long.parseLong(args[4]);
        final long clockIncrement = Long.parseLong(args[5]);

        processClock = new ProcessClock(startTime);

        // Incrementa o tempo do processo em X de 1 em 1 segundo
        new Thread(() -> {
            while (true) {
                try {
                    //System.out.println("Hora Anterior: " + processClock.getTime());
                    Thread.sleep(1000);
                    processClock.incrementTime(clockIncrement);
                    // System.out.println("Hora Atual: " + processClock.getTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (args.length != 6) {
            System.out.println("The program should run with 6 arguments. i.e: java Process <processId> <host> <port> <time> <ptime> <adelay>");
        }

        // Inicia os processos mestre e escravo
        if (processId == 0) {
            System.out.println("I'm the boss");
            bossProcess(host, processPort);
        } else {
            System.out.println("I'm a intern");
            slaveProcess(host, processPort);
        }

    }

    private static void bossProcess(String host, int port) {
        try {
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);

            multicastSocket.joinGroup(multicastAddress);

            // recebe as horas do estag
            DatagramSocket unicastSocket = new DatagramSocket(MASTER_PORT);
            // Fluxo principal

            while (true) {
                String message = "Give me the time!";

                // envia mensagem pedindo o tempo dos escravos em multicast
                byte[] buffer = message.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, multicastAddress, MULTICAST_PORT);
                multicastSocket.send(datagramPacket);

                // recebe resposta
                byte[] answerBuffer = new byte[256];
                DatagramPacket answerPacket = new DatagramPacket(answerBuffer, answerBuffer.length);
                multicastSocket.receive(answerPacket);
                String answer = new String(answerPacket.getData(), 0, answerPacket.getLength());
                System.out.println("Received message from master: " + answer);

                // Adiciona sua própria hora na lista
                processes.add(new Process(0, host, port, processClock.getTime()));

                // recebe a hora dos escravos
                while (processes.size() < 2 /* número de escravos*/) {
                    byte[] abuffer = new byte[256];
                    DatagramPacket slavePacket = new DatagramPacket(abuffer, abuffer.length);
                    //System.out.println("Aguardando mensagem");

                    // Tempo para o mestre poder inicilizar a espera de uma comunicação unicast (usaremos como descompasso do mestre)
                    unicastSocket.receive(slavePacket);

                    //System.out.println("mensagem recebida");

                    String[] receivedMessage = new String(slavePacket.getData(), 0, slavePacket.getLength()).split(";");
                    System.out.println("Received message: "+ Arrays.toString(receivedMessage));

                    // "id;comando;tempo"
                    int id = Integer.parseInt(receivedMessage[0]);
                    String command = receivedMessage[1];

                    if (!command.equals("mytime")) {
                        continue;
                    }

                    long time = Long.parseLong(receivedMessage[2]);

                    var process = new Process(id, slavePacket.getAddress().getHostAddress(), slavePacket.getPort(), time);

                    processes.add(process);
                    System.out.println("Process info received: " + process);
                    System.out.println("Time: " + processClock.getTime());
                }

                //unicastSocket.close();

                boolean repeatAverageCalc = true;

                included.addAll(processes);
                //Verifica se precisa calcular uma nova média

                long average = 0;
                while (repeatAverageCalc) {

                    // Se todos forem destoantes, pega o processo do mestre
                    if (included.isEmpty()) {
                        average = processClock.getTime();
                        break;
                    }

                    long somaTudo = 0;

                    //Faz a media da lista dos escravos
                    for (Process p : included) {
                        somaTudo += p.getCurrentTime();
                    }

                    average = somaTudo / included.size();
                    System.out.println("Média calculada: " + average);

                    int includedSize = included.size();

                    long finalAverage = average;
                    included.removeIf(p -> Math.abs(p.getCurrentTime() - finalAverage) > MAX_TOLERATION);

                    repeatAverageCalc = includedSize != included.size();
                }

                for (Process process : processes) {
                    var timeToAdjust = average - process.getCurrentTime();

                    if (process.getId() == 0) {
                        processClock.incrementTime(timeToAdjust);
                    } else {

                        var timeAdjustMessage = 0 + ";adjusttime;" + timeToAdjust;

                        byte[] timeAdjustBuffer = timeAdjustMessage.getBytes();

                        var packet = new DatagramPacket(timeAdjustBuffer, timeAdjustBuffer.length,
                                InetAddress.getByName(process.getHost()), process.getPort());

                        unicastSocket.send(packet);
                    }
                }

                Thread.sleep(5000); // 30 segundos na versão final

                processes.clear();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void slaveProcess(String host, int port) {

        try {
            DatagramSocket unicastSocket = new DatagramSocket(processPort);

            new Thread(() -> {
                try {
                    sendTime(port, unicastSocket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Thread que ajusta o próprio tempo
            new Thread(() -> {
                try {
                    byte[] abuffer = new byte[256];
                    DatagramPacket slavePacket = new DatagramPacket(abuffer, abuffer.length);
                    System.out.println("Aguardando mensagem");

                    // Tempo para o mestre poder inicilizar a espera de uma comunicação unicast (usaremos como descompasso do mestre)
                    unicastSocket.receive(slavePacket);


                    System.out.println("mensagem recebida");
                    String[] receivedMessage = new String(slavePacket.getData(), 0, slavePacket.getLength()).split(";");

                    String command = receivedMessage[1];

                    long time = Long.parseLong(receivedMessage[2]);

                    if (command.equals("adjusttime")) {
                        processClock.incrementTime(time);
                        System.out.println("Tempo ajustado, novo tempo: " + processClock.getTime());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendTime(int port, DatagramSocket unicastSocket) throws IOException, InterruptedException {
        InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastSocket.joinGroup(multicastAddress);

        while (true) {
            byte[] buffer = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            multicastSocket.receive(datagramPacket);
            String receivedMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            System.out.println("Received message from master: " + receivedMessage);

            // Processa a mensagem recebida do mestre e gera a resposta
            System.out.println("Slave time: " + processClock.getTime());

            // "id;comando;tempo"
            String answer = processId + ";mytime;" + processClock.getTime();

            byte[] answerBuffer = answer.getBytes();
            System.out.println("Gerando mensagem para ser enviada");

            Thread.sleep(10);

            DatagramPacket answerPacket = new DatagramPacket(answerBuffer, answerBuffer.length,
                    datagramPacket.getAddress(), MASTER_PORT);

            unicastSocket.send(answerPacket);
            System.out.println("Mensagem enviada para: " + answerPacket.getAddress().getHostAddress() + ":" + answerPacket.getPort());


        }
    }

    private static boolean todasRespostasRecebidas() {
        // Implemente a lógica para verificar se todas as respostas foram recebidas
        // Neste exemplo, consideraremos que todas as respostas foram recebidas
        return true;
    }
}
