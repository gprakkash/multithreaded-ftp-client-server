package client;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

/*
Data Channel - Client Endpoint
 */
public class DataClient extends Thread{
    private static int threadCounter = 0;
    private Socket dataClientSocket = null;
    private DataInputStream dataIs = null;
    private PrintStream dataOs = null;
    private boolean stor = false;
    private boolean retr = false;
    private FileOutputStream fileOpS = null;
    private BufferedOutputStream buffOpS = null;

    private FileInputStream fileIpS = null;
    private BufferedInputStream buffIpS = null;
    private String retrFilePath = null;
    private String storFilePath = null;
    private int retrFileLength = 0;
    private boolean taskComplete = false;


    private static synchronized int getThreadCounter() {
        return threadCounter++;
    }

    public DataClient(Socket dataClientSocket){
        super(Integer.toString(getThreadCounter()));
        this.dataClientSocket = dataClientSocket;
    }

    @Override
    public void run(){
        /*
        Create input and output streams for this client.
         */
        try {
            dataIs = new DataInputStream(dataClientSocket.getInputStream());
            dataOs = new PrintStream(dataClientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
        try {
            if (stor == true && retr == false) {
                upload();
            } else if (stor == false && retr == true) {
                download();
            }
            taskComplete = true;
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        } finally {
            System.out.println("Data Channel for Thread: " + Thread.currentThread().getName() + " is closing.");
            cleanUp();
        }
    }

    /*
    send file to Data Channel - Server Endpoint
     */
    private void upload() throws IOException{
        try {
            File fileReference = new File(storFilePath);
            byte[] myByteArray = new byte[(int) fileReference.length()];
            fileIpS = new FileInputStream(fileReference);
            buffIpS = new BufferedInputStream(fileIpS);
            buffIpS.read(myByteArray,0,myByteArray.length);
            Thread.sleep(10000);
            dataOs.write(myByteArray,0,myByteArray.length);
            dataOs.flush();
//            System.out.println("File upload done");
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException: " + e);
        } finally {
            if (fileIpS != null) fileIpS.close();
            if (buffIpS != null) buffIpS.close();
        }
    }

    /*
    receive file from Data Channel - Server Endpoint
     */
    private void download() throws IOException{
        try {
            // receive file
            byte[] myByteArray = new byte[retrFileLength];
            fileOpS = new FileOutputStream(retrFilePath);
            buffOpS = new BufferedOutputStream(fileOpS);
            dataIs.read(myByteArray,0, retrFileLength);
            Thread.sleep(10000);
            buffOpS.write(myByteArray, 0 , retrFileLength);
            buffOpS.flush();
//            System.out.println("File download done.");
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException: " + e);
        } finally {
            if (fileOpS != null) fileOpS.close();
            if (buffOpS != null) buffOpS.close();
        }
    }

    public void setStor(boolean bool) {
        stor = bool;
        retr = !bool;
    }

    public void setRetr(boolean bool) {
        retr = bool;
        stor = !bool;
    }

    public void setRetrFilePath(String retrFilePath) {
        this.retrFilePath = retrFilePath;
    }

    public void setStorFilePath(String storFilePath) {
        this.storFilePath = storFilePath;
    }

    public void setRetrFileLength(int retrFileLength) {
        this.retrFileLength = retrFileLength;
    }

    public boolean isTaskComplete() {
        return taskComplete;
    }

    private void cleanUp() {
        try {
            if (dataIs != null)
                dataIs.close();
            if (dataOs != null)
                dataOs.close();
            if (dataClientSocket != null)
                dataClientSocket.close();
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }

}
