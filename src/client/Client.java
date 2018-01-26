package client;

import utility.Constants;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client{

    private static Socket controlClientSocket = null; // the client socket for control channel

    public static void main(String[] args) {
        /*
         * For a Control Channel:
         * Open a socket on a given host and port.
         */
        try {
            controlClientSocket = new Socket(Constants.HOST_NAME, Constants.CONTROL_PORT);
        } catch (UnknownHostException e) {
            System.err.println("Don't know the host " + Constants.HOST_NAME);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host "
                    + Constants.HOST_NAME);
        }

        new Thread(new ControlClient(controlClientSocket)).start();
    }

}
