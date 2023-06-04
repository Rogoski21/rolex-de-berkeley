package org.reprovados;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class Berkeley {

    private static final int MULTICAST_PORT = 8888;
    private static final int MASTER_PORT = 8000;
    private static final String MULTICAST_ADDRESS = "239.0.0.1";

    private static final List<Process> processes = new ArrayList<>();

    public static void main(String[] args) {
        int processId = Integer.parseInt(args[0]);
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        long startTime = Long.parseLong(args[3]);
        long processTime = Long.parseLong(args[4]);
        long aDelay = Long.parseLong(args[5]);

        if (args.length != 6) {
            System.out.println("The program should run with 6 arguments. i.e: java Process <processId> <host> <port> <time> <ptime> <adelay>");
        }

        if(processId == 0) {
            System.out.println("I'm the master");
            masterProcess();
        } else {
            System.out.println("I'm a slave");
            slaveProcess(port, startTime);
        }

    }

    private static void masterProcess() {
        try {
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);

            multicastSocket.joinGroup(multicastAddress);

            String message = "Hi, slaves!";
            byte[] buffer = message.getBytes();

            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, multicastAddress, MULTICAST_PORT);
            multicastSocket.send(datagramPacket);

            byte[] answerBuffer = new byte[256];
            DatagramPacket answerPacket = new DatagramPacket(answerBuffer, answerBuffer.length);

            while (true) {
                multicastSocket.receive(answerPacket);
                String resposta = new String(answerPacket.getData(), 0, answerPacket.getLength());
                System.out.println("Received message from slave: " + resposta);

                // Verifica se todas as respostas foram recebidas
                if (todasRespostasRecebidas()) {
                    break;
                }
            }

            while (true) {
                DatagramSocket unicastSocket = new DatagramSocket(MASTER_PORT);
                byte[] abuffer = new byte[256];
                DatagramPacket slavePacket = new DatagramPacket(abuffer, abuffer.length);

                unicastSocket.receive(slavePacket);
                String receivedMessage = new String(slavePacket.getData(), 0, slavePacket.getLength());

                var process = new Process(slavePacket.getAddress().getHostAddress(), slavePacket.getPort(), Integer.parseInt(receivedMessage));

                processes.add(process);
                System.out.println("Process info received: " + process);
                break;
            }

            multicastSocket.leaveGroup(multicastAddress);
            multicastSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void slaveProcess(int port, long startTime) {
        try {
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);

            multicastSocket.joinGroup(multicastAddress);

            byte[] buffer = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            multicastSocket.receive(datagramPacket);
            String receivedMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
            System.out.println("Received message from master: " + receivedMessage);

            // Processa a mensagem recebida do mestre e gera a resposta
            String answer = String.valueOf(startTime);
            byte[] answerBuffer = answer.getBytes();

            DatagramSocket unicastSocket = new DatagramSocket(port);
            DatagramPacket answerPacket = new DatagramPacket(answerBuffer, answerBuffer.length,
                    datagramPacket.getAddress(), MASTER_PORT);
            unicastSocket.send(answerPacket);

            unicastSocket.close();
            multicastSocket.leaveGroup(multicastAddress);
            multicastSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean todasRespostasRecebidas() {
        // Implemente a lógica para verificar se todas as respostas foram recebidas
        // Neste exemplo, consideraremos que todas as respostas foram recebidas
        return true;
    }
}
