package com.chat.demo;

import java.io.*;
import javax.net.ssl.SSLSocket;

/**
 * Handler đơn giản cho mỗi client connection
 */
public class DemoClientHandler implements Runnable {
    private SSLSocket socket;

    public DemoClientHandler(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            Object obj;
            while ((obj = ois.readObject()) != null) {
                System.out.println("Received: " + obj);
                oos.writeObject("Echo: " + obj);
                oos.flush();
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } catch (Exception e) {
            System.err.println("Handler error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
