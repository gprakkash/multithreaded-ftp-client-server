package server;

import utility.FileAccessManager;
import utility.MutexLock;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

public class DataServer extends Thread{
    private Socket dataServerSocket = null;
    private DataInputStream dataIs = null;
    private PrintStream dataOs = null;
    private boolean stor = false;
    private boolean retr = false;

    private FileInputStream fileIpS = null;
    private BufferedInputStream buffIpS = null;
    private FileOutputStream fileOpS = null;
    private BufferedOutputStream buffOpS = null;
    private String storFilePath = null;
    private int storFileLength = 0;
    private String retrFilePath = null;
    private int retrFileLength = 0;
    private boolean taskComplete = false;

    public DataServer(Socket dataServerSocket) {
        this.dataServerSocket = dataServerSocket;
    }
    @Override
    public void run() {
        /*
        Create input and output streams for this client.
         */
        try {
            dataIs = new DataInputStream(dataServerSocket.getInputStream());
            dataOs = new PrintStream(dataServerSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }

        try {
            if (stor == true && retr == false) {
                // STOR
                download();
            } else if (stor == false && retr == true) {
                // RETR
                upload();
            }
            taskComplete = true;
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        } finally {
            System.out.println("Data Channel is closing");
            cleanUp();
        }
    }

    /*
    send file to Data Channel - Client Endpoint
     */
    private void upload() throws IOException{
        MutexLock lock = FileAccessManager.getFileLock(retrFilePath);
        try {
            lock.lockRead();
            // delay
            try {
                System.out.println(Thread.currentThread().getName() + " going on sleep");
                Thread.sleep(15000);
                System.out.println(Thread.currentThread().getName() + " woke up");
            } catch (InterruptedException e) {}

            File fileReference = new File(retrFilePath);
            byte[] myByteArray = new byte[(int) fileReference.length()];
            fileIpS = new FileInputStream(fileReference);
            buffIpS = new BufferedInputStream(fileIpS);
            buffIpS.read(myByteArray,0,myByteArray.length);
            dataOs.write(myByteArray,0,myByteArray.length);
            dataOs.flush();
            System.out.println("File upload done");
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        } catch (InterruptedException e){
            System.err.println("InterruptedException: " + e);
        } finally {
            lock.unlockRead();
            if (fileIpS != null) fileIpS.close();
            if (buffIpS != null) buffIpS.close();
        }
    }

    /*
    receive file from Data Channel - Client Endpoint
     */
    private void download() throws IOException{
        MutexLock lock = null;
        // check if lock exists
        synchronized (FileAccessManager.class) {
            if (FileAccessManager.isFilePresent(storFilePath)) {
                lock = FileAccessManager.getFileLock(storFilePath);
            } else {
                // if it doesn't exist, create and assign the lock
                FileAccessManager.createLock(storFilePath);
                lock = FileAccessManager.getFileLock(storFilePath);
            }
        }
        try {
            lock.lockWrite();
            // delay
            try {
                System.out.println(Thread.currentThread().getName() + " going on sleep");
                Thread.sleep(10000);
                System.out.println(Thread.currentThread().getName() + " woke up");
            } catch (InterruptedException e) {}

            // receive file
            byte[] myByteArray = new byte[storFileLength];
            fileOpS = new FileOutputStream(storFilePath);
            buffOpS = new BufferedOutputStream(fileOpS);
//            System.out.println("length of the file to be received: " + storFileLength);
            dataIs.read(myByteArray,0, storFileLength);

            buffOpS.write(myByteArray, 0 , storFileLength);
            buffOpS.flush();
            System.out.println("File download done.");
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        } catch (InterruptedException e){
            System.err.println("InterruptedException: " + e);
        } finally {
            try {
                lock.unlockWrite();
            } catch (InterruptedException e) {
                System.err.println("InterruptedException: " + e);
            }
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

    public void setStorFilePath(String storFilePath) {
        this.storFilePath = storFilePath;
    }

    public void setRetrFilePath(String retrFilePath) {
        this.retrFilePath = retrFilePath;
    }

    public void setStorFileLength(int storFileLength) {
        this.storFileLength = storFileLength;
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
            if (dataServerSocket != null)
                dataServerSocket.close();
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
}