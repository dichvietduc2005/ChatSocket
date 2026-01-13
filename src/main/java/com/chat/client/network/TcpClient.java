package com.chat.client.network;

import com.chat.common.protocol.NetworkConstants;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.net.*;
import java.io.IOException;

public class TcpClient {
    // UDP Buzz components
    private DatagramSocket buzzSocket;
    private Thread buzzListenerThread;
    private Stage primaryStage;
    private boolean isListening = false;

    // Multicast components
    private MulticastSocket multicastSocket;
    private Thread multicastListenerThread;
    private boolean isMulticastListening = false;

    // ===== UDP BUZZ FEATURE =====

    public void initBuzzListener(Stage stage) {
        this.primaryStage = stage;
        try {
            buzzSocket = new DatagramSocket(NetworkConstants.UDP_BUZZ_PORT);
            isListening = true;

            buzzListenerThread = new Thread(this::listenForBuzz);
            buzzListenerThread.setDaemon(true);
            buzzListenerThread.start();

            System.out.println("UDP Buzz listener started on port " + NetworkConstants.UDP_BUZZ_PORT);
        } catch (SocketException e) {
            System.err.println("Failed to start UDP Buzz listener: " + e.getMessage());
        }
    }

    private void listenForBuzz() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (isListening && !buzzSocket.isClosed()) {
            try {
                buzzSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                if ("BUZZ".equals(message.trim())) {
                    Platform.runLater(() -> {
                        if (primaryStage != null) {
                            vibrateWindow(primaryStage);
                        }
                    });

                    playBuzzSound();

                    System.out.println("Received BUZZ from " + packet.getAddress());
                }
            } catch (IOException e) {
                if (isListening) {
                    System.err.println("Error receiving BUZZ packet: " + e.getMessage());
                }
            }
        }
    }

    private void vibrateWindow(Stage stage) {
        double originalX = stage.getX();
        double originalY = stage.getY();
        int vibrationCount = 10;
        int vibrationDistance = 5;

        new Thread(() -> {
            try {
                for (int i = 0; i < vibrationCount; i++) {
                    final int offset = (i % 2 == 0) ? vibrationDistance : -vibrationDistance;
                    Platform.runLater(() -> {
                        stage.setX(originalX + offset);
                        stage.setY(originalY + offset);
                    });
                    Thread.sleep(20);
                }
                Platform.runLater(() -> {
                    stage.setX(originalX);
                    stage.setY(originalY);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void playBuzzSound() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    return;
                }

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                int sampleRate = 44100;
                int frequency = 800;
                int duration = 200;
                int numSamples = sampleRate * duration / 1000;

                byte[] buffer = new byte[numSamples * 2];
                for (int i = 0; i < numSamples; i++) {
                    double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                    short sample = (short) (Short.MAX_VALUE * 0.5 * Math.sin(angle));
                    buffer[i * 2] = (byte) (sample & 0xFF);
                    buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }

                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }).start();
    }

    public void sendBuzz(String targetIP) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = "BUZZ";
            byte[] data = message.getBytes();
            InetAddress targetAddress = InetAddress.getByName(targetIP);

            DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                targetAddress,
                NetworkConstants.UDP_BUZZ_PORT
            );

            socket.send(packet);
            System.out.println("Sent BUZZ to " + targetIP);
        } catch (IOException e) {
            System.err.println("Failed to send BUZZ: " + e.getMessage());
        }
    }

    public void stopBuzzListener() {
        isListening = false;
        if (buzzSocket != null && !buzzSocket.isClosed()) {
            buzzSocket.close();
        }
        if (buzzListenerThread != null) {
            buzzListenerThread.interrupt();
        }
    }

    // ===== MULTICAST LISTENER =====

    public void startMulticastListener(TextArea notificationArea) {
        try {
            multicastSocket = new MulticastSocket(NetworkConstants.MULTICAST_PORT);
            InetSocketAddress group = new InetSocketAddress(
                InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS),
                NetworkConstants.MULTICAST_PORT
            );
            multicastSocket.joinGroup(group, null);

            isMulticastListening = true;
            multicastListenerThread = new Thread(() -> listenForMulticast(notificationArea));
            multicastListenerThread.setDaemon(true);
            multicastListenerThread.start();

            System.out.println("Multicast listener started on " + NetworkConstants.MULTICAST_ADDRESS);
        } catch (IOException e) {
            System.err.println("Failed to start multicast listener: " + e.getMessage());
        }
    }

    private void listenForMulticast(TextArea notificationArea) {
        byte[] buffer = new byte[1024];

        while (isMulticastListening && multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("ADMIN:")) {
                    String adminMessage = message.substring(6);

                    Platform.runLater(() -> {
                        if (notificationArea != null) {
                            notificationArea.appendText("🔔 " + adminMessage + "\n");
                        }
                    });

                    System.out.println("Received admin notification: " + adminMessage);
                }
            } catch (IOException e) {
                if (isMulticastListening) {
                    System.err.println("Error receiving multicast: " + e.getMessage());
                }
            }
        }
    }

    public void stopMulticastListener() {
        isMulticastListening = false;
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                InetSocketAddress group = new InetSocketAddress(
                    InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS),
                    NetworkConstants.MULTICAST_PORT
                );
                multicastSocket.leaveGroup(group, null);
            } catch (IOException e) {
                System.err.println("Error leaving multicast group: " + e.getMessage());
            }
            multicastSocket.close();
        }
        if (multicastListenerThread != null) {
            multicastListenerThread.interrupt();
        }
    }
}
