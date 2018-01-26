package client;

import utility.Constants;
import utility.FileHandler;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/*
Control Channel - Client Endpoint
 */
public class ControlClient implements Runnable{
    private Socket controlClientSocket = null;
    private DataInputStream controlIs = null;
    private PrintStream controlOs = null;

    private BufferedReader inputFromConsole = null;
    private boolean closed = false;
    ArrayList<DataClient> dataThreads = new ArrayList<DataClient>();

    public ControlClient(Socket controlClientSocket){
        this.controlClientSocket = controlClientSocket;
    }

    public void run(){
        inputFromConsole = new BufferedReader(new InputStreamReader(System.in));
        /*
        Create input and output streams for this client.
         */
        try {
            controlIs = new DataInputStream(controlClientSocket.getInputStream());
            controlOs = new PrintStream(controlClientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        try {
            while (!authenticated()) {}
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        try {
            System.out.println("Control Channel is up.");
            while (!closed) {
                menu();
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

    private void menu() throws IOException
    {
        System.out.println("\n[ MENU ]");
        System.out.println("STOR");
        System.out.println("RETR");
        System.out.println("PORT");
        System.out.println("NOOP");
        System.out.println("TYPE");
        System.out.println("MODE");
        System.out.println("LIST");
        System.out.println("PWD");
        System.out.println("QUIT");

        System.out.print("Enter Choice :\n");

        String choice = inputFromConsole.readLine().trim();

        if (choice.equals("STOR")) {
            stor();
        }
        else if (choice.equals("RETR")) {
            sendToControlServer("RETR");
            retr();
        }
        else if (choice.equals("PORT")) {
            sendToControlServer("PORT");
            getAndPrintResponseFromControlServer();
        }
        else if (choice.equals("NOOP")) {
            sendToControlServer("NOOP");
            getAndPrintResponseFromControlServer();
        }
        else if (choice.equals("TYPE")) {
            sendToControlServer("TYPE");
            getAndPrintResponseFromControlServer();
        }
        else if (choice.equals("MODE")) {
            sendToControlServer("MODE");
            getAndPrintResponseFromControlServer();
        }
        else if (choice.equals("LIST")) {
            sendToControlServer("LIST");
            // list server files
            String fileName;
            while(!(fileName = getResponseFromControlServer()).equals(Constants.OK)){
                System.out.println("File: " + fileName);
            }
        }
        else if (choice.equals("PWD")) {
            sendToControlServer("PWD");
            getAndPrintResponseFromControlServer();
        }
        else if (choice.equals("QUIT")) {
            sendToControlServer("QUIT");
            getAndPrintResponseFromControlServer();
            closed = true;
        }
        else if (choice.equals("")) {
            System.out.println("****");
            sendToControlServer("DEMO");
        }
        else {
            System.out.println("Incorrect choice.\n");
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

    private DataClient setupDataChannel(){

        removeCompletedThreads();

        DataClient dataThread;
        Socket dataClientSocket = null;

        /*
        For the Data Channel:
        Open a socket on a given host and port.
         */
        try {
            do {
                if (dataClientSocket != null)
                    dataClientSocket.close();
                dataClientSocket = new Socket(Constants.HOST_NAME, Constants.DATA_PORT);
            } while (!dataConnectionEstablished()); // keep trying until received true from server

        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException:  " + e);
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        dataThread = new DataClient(dataClientSocket);
        dataThreads.add(dataThread);
        return dataThread;
    }

    private void stor() throws IOException{

        FileHandler.listFiles(FileHandler.getCurrDir()+ File.separator + "clientfiles");
        System.out.println("Filename: ");
        String storFileName = inputFromConsole.readLine().trim();
        String storFilePath = FileHandler.getPath("clientfiles" + File.separator + storFileName);
        File fileReference = new File(storFilePath);
        if (!fileReference.exists()) {
            System.out.println("File not found");
            return;
        }
        // only if the fileName is correct we will ask the server to proceed
        sendToControlServer("STOR");

        /*
        Setup Data Channel
         */
        DataClient dataThread = setupDataChannel();

        dataThread.setStorFilePath(storFilePath); // send complete path to Data Thread
        dataThread.setStor(true);
        // send filename to the Control Channel - Server Endpoint
        sendToControlServer(storFileName);
        // file length
        String storFileLength = Long.toString(fileReference.length());
        sendToControlServer(storFileLength);
        dataThread.start();
    }

    private void retr() throws IOException{

        /*
        Setup Data Channel
         */
        DataClient dataThread = setupDataChannel();

        // list server files
        String fileName;
        while(!(fileName = getResponseFromControlServer()).equals(Constants.OK)){
            System.out.println("File: " + fileName);
        }

        System.out.println("Filename: ");
        String retrFileName = inputFromConsole.readLine().trim();
        // send filename to the Control Channel - Server Endpoint
        sendToControlServer(retrFileName);

        // return if file doesn't exist on server
        if (!getResponseFromControlServer().equals(Constants.SUCCESS)) {
            System.out.println("File not found on server.");
            return;
        }

        // receive file length
        String retrFileLength = getResponseFromControlServer();

        String retrFilePath = FileHandler.getPath("clientfiles" + File.separator + retrFileName);


        dataThread.setRetrFileLength(Integer.parseInt(retrFileLength));
        dataThread.setRetrFilePath(retrFilePath); // send complete path to Data Thread
        dataThread.setRetr(true);
        dataThread.start();
    }

    /*
    get response from Control Channel - Server Endpoint, and Print
     */
    private void getAndPrintResponseFromControlServer() throws IOException{
        String responseFromServer = null;
        responseFromServer = controlIs.readLine();
        System.out.println(responseFromServer);
    }


    /*
    get response from Control Channel - Server Endpoint
     */
    private String getResponseFromControlServer() throws IOException{
        return controlIs.readLine();
    }

    /*
    send String message to Control Channel - Server Endpoint
     */
    private void sendToControlServer(String msg) throws IOException {
        controlOs.println(msg);
    }

    /*
    the server sends true or false over control channel
    if the data channel was successfully established
    @return status received from the server
     */
    private boolean dataConnectionEstablished() throws IOException{
        boolean status = getResponseFromControlServer().equals(Constants.SUCCESS);
        return status;
    }

    private boolean authenticated() throws IOException{
        System.out.println("USER <username>");
        String login = inputFromConsole.readLine().trim();
        sendToControlServer(login);
        // check if the file exist
        if(!getResponseFromControlServer().equals(Constants.SUCCESS))
            return false;
        getAndPrintResponseFromControlServer();
        String pass = inputFromConsole.readLine().trim();
        sendToControlServer(pass);
        return getResponseFromControlServer().equals(Constants.SUCCESS);
    }



    private void cleanUp() {
        try {
            if (controlIs != null)
                controlIs.close();
            if (controlOs != null)
                controlOs.close();
            if (controlClientSocket != null)
                controlClientSocket.close();
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
}