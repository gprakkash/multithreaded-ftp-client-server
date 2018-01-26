package utility;

import java.io.File;
import java.io.IOException;

public class FileHandler {
    /*
    list files
     */
    public static void listFiles(String dir) {
        if (dir == null) {
            System.out.println("Path missing.");
            return;
        }
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile())
                System.out.println("File: " + listOfFiles[i].getName());
        }
    }

    /*
    return absolute path of current directory, with appended file name
     */
    public static String getPath(String filename) {
        try {
            String current = new File(".").getCanonicalPath();
            return (current + File.separator + filename);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        }
        return null;
    }

    /*
    return absolute path of current directory, with appended file name
     */
    public static String getCurrDir() {
        try {
            String current = new File(".").getCanonicalPath();
            return current;
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        }
        return null;
    }
}
