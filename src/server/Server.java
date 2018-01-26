package server;

import utility.Constants;
import utility.FileAccessManager;
import utility.FileHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;

public class Server {
    private static ServerSocket ftpControl = null;
    public static ServerSocket ftpData = null;
    private static Socket controlServerSocket = null;
    public static Map<String, String> usersAndPass = null;
    public static void main(String[] args) {
        /*
        Initialize FileAccessRecord
         */
        FileAccessManager.initializeFileAccessRecord();

        /*
        Open a server socket on the port number 21
         */
        try {
            ftpControl = new ServerSocket(Constants.CONTROL_PORT);
            ftpData = new ServerSocket(Constants.DATA_PORT);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }

        /*
        Stores the username and passwords in a map for authentication in Control Channel - Server Endpoint
         */
        initializeUsersAndPass();

        /*
        Create a client socket for each connection and pass it to a new ControlServer thread
         */
        while (true) {
            try {
                controlServerSocket = ftpControl.accept();
                System.out.println("Client - Control Endpoint: " + controlServerSocket.getInetAddress());
                new Thread(new ControlServer(controlServerSocket)).start();
            } catch (IOException e) {
                System.err.println("IOException" + e);
            }
        }
    }

    public static void initializeUsersAndPass() {
        usersAndPass = new HashMap<String,String>();
        String file = "files/creds.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String user;
            while ((user = br.readLine()) != null) {
                String pass = br.readLine();
                usersAndPass.put(user, pass);
            }
        } catch (IOException e) {
            System.err.println("IOException" + e);
        }
    }
}
