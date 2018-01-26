package server;

import utility.Constants;
import utility.FileHandler;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ControlServer implements Runnable{
    private ServerSocket ftpData = null;
    private Socket controlServerSocket = null;
    private DataInputStream controlIs = null;
    private PrintStream controlOs = null;

    private boolean closed = false;
    ArrayList<DataServer> dataThreads = new ArrayList<DataServer>();

    public ControlServer(Socket controlServerSocket){
        this.controlServerSocket = controlServerSocket;
    }

    public void run() {
        /*
        Create input and output streams for this controlServerSocket.
        */
        try {
            controlIs = new DataInputStream(controlServerSocket.getInputStream());
            controlOs = new PrintStream(controlServerSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        try {
            while (!authenticated()) {
                System.out.println("Authentication Failure: " + controlServerSocket.getInetAddress());
            }
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }


        try {
            System.out.println("Control Channel is up.");
            while (!closed) {
                respond();
            }
            removeCompletedThreads();
            if (dataThreads.size() != 0) {
                System.out.println("Few data threads still running.");
            }
            System.out.println("Control Channel is closing");
            cleanUp();
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

    }

    private void respond() throws IOException
    {
        String choice;
        choice = getResponseFromControlClient();
        if (choice.equals("STOR")) {
            stor();
        }
        else if (choice.equals("RETR")) {
            retr();
        }
        else if (choice.equals("PORT")) {
            sendToControlClient(">>DATA port: 20");
        }
        else if (choice.equals("NOOP")) {
            sendToControlClient(">>OK");
        }
        else if (choice.equals("TYPE")) {
            sendToControlClient(">>ASCII TYPE");
        }
        else if (choice.equals("MODE")) {
            sendToControlClient(">>STREAM mode");
        }
        else if (choice.equals("LIST")) {
            sendFileNamesToClient();
        }
        else if (choice.equals("PWD")) {
            sendToControlClient(">>" + FileHandler.getCurrDir());
        }
        else if (choice.equals("QUIT")) {
            sendToControlClient(">>QUIT");
            closed = true;
        }
        else if (choice.equals("DEMO")) {
            System.out.println();
            System.out.println("******************************");
        }
        else {
            System.out.println("Incorrect choice from Client");
        }
    }

    private void removeCompletedThreads() {
        ArrayList<Integer> removeIndex = new ArrayList<Integer>();
        /*
        Remove dataThread entry that have completed its task
         */
        for (int i = 0; i < dataThreads.size(); i++) {
            if (dataThreads.get(i).isTaskComplete())
                removeIndex.add(i);
        }

        for (int i= 0; i < removeIndex.size(); i++) {
            dataThreads.remove(removeIndex.get(i));
        }
    }


    private DataServer setupDataChannel(){
        removeCompletedThreads();

        DataServer dataThread;
        Socket dataServerSocket = null;
        /*
        For the Data Channel, open a socket.
         */
        try {
            ftpData = Server.ftpData;
            do {
                if (dataServerSocket != null)
                    dataServerSocket.close();
                dataServerSocket = ftpData.accept();
                System.out.println("Client - Data Endpoint: " + dataServerSocket.getInetAddress());
            } while(!dataServerSocket.getInetAddress().equals(controlServerSocket.getInetAddress()));
            sendToControlClient(Constants.SUCCESS); // Data Channel Established

        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        dataThread = new DataServer(dataServerSocket);
        dataThreads.add(dataThread);
        return dataThread;
    }

    private void stor() throws IOException {
        /*
        Setup Data Channel
         */
        DataServer dataThread = setupDataChannel();

        // receive filename from the Control Channel - Client Endpoint
        String storFileName = getResponseFromControlClient();
        String storFileLength = getResponseFromControlClient();
        String storFilePath = FileHandler.getPath("serverfiles" + File.separator + storFileName);
        dataThread.setStorFileLength(Integer.parseInt(storFileLength)); // send complete path to Data Thread
        dataThread.setStorFilePath(storFilePath); // send complete path to Data Thread
        dataThread.setStor(true);

        dataThread.start();
    }

    private void retr() throws IOException {
        /*
        Setup Data Channel
         */
        DataServer dataThread = setupDataChannel();

        // send file list to the Control Channel - Client Endpoint
        sendFileNamesToClient();

        String retrFileName = getResponseFromControlClient();
        String retrFilePath = FileHandler.getPath("serverfiles" + File.separator + retrFileName);

        File fileReference = new File(retrFilePath);
        if (!fileReference.exists()) {
            sendToControlClient(Constants.FAILURE);
            return;
        } else {
            sendToControlClient(Constants.SUCCESS);
        }

        // send file length
        sendToControlClient(Long.toString(fileReference.length()));

        dataThread.setRetr(true);
        dataThread.setRetrFilePath(retrFilePath);

        dataThread.start();
    }
    private void sendFileNamesToClient() throws IOException{
        String dir = FileHandler.getCurrDir() + File.separator + "serverfiles";
        if (dir == null) {
            System.out.println("Path missing.");
            return;
        }
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile())
                sendToControlClient(listOfFiles[i].getName());
        }
        sendToControlClient(Constants.OK);
    }

    /*
    get response from Control Channel - Client Endpoint
     */
    private String getResponseFromControlClient() throws IOException{
        String responseFromClient = null;
        responseFromClient = controlIs.readLine();
        return responseFromClient;
    }

    /*
    send String message to Control Channel - Client Endpoint
     */
    private void sendToControlClient(String msg) throws IOException {
        controlOs.println(msg);
    }

    private boolean authenticated() throws IOException{
        String login = getResponseFromControlClient();
        String[] parts = login.split(" ");
        if (parts.length == 2) {
            String user = parts[1];
            if (parts[0].equals("USER")) {
                if (Server.usersAndPass.containsKey(user)) {
                    sendToControlClient(Constants.SUCCESS);
                    sendToControlClient("password: ");
                    String pass = getResponseFromControlClient();
                    if (pass.equals(Server.usersAndPass.get(parts[1]))) {
                        sendToControlClient(Constants.SUCCESS);
                        return true;
                    }
                }
            }
        }
        sendToControlClient(Constants.FAILURE);
        return false;
    }

    private void cleanUp() {
        try {
            if (controlIs != null)
                controlIs.close();
            if (controlOs != null)
                controlOs.close();
            if (controlServerSocket != null)
                controlServerSocket.close();
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
}
