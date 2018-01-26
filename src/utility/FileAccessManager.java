package utility;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileAccessManager {
	public static Map <String, MutexLock> FileAccessRecord = new HashMap<String, MutexLock>();

	public static boolean isFilePresent(String filePath){
		if (FileAccessRecord.containsKey(filePath)){
			return true;
		}
		return false;
	}
	public static MutexLock getFileLock(String filePath){
		MutexLock ml = FileAccessRecord.get(filePath);
		return ml;
	}
	
	public static void deleteFileLock(String filePath){
		FileAccessRecord.remove(filePath);
	}
	
	public static void createLock(String filePath){
		FileAccessRecord.put(filePath, new MutexLock());
	}

	/*
    list files
     */
	public static void initializeFileAccessRecord() {
        String dir = FileHandler.getCurrDir() + File.separator + "serverfiles";
        if (dir == null) {
            System.out.println("No files in the serverfiles to initialize");
            return;
        }
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
//                sendToControlClient();
                String fileName = listOfFiles[i].getName();
                String filePath = FileHandler.getPath("serverfiles" + File.separator + fileName);
                createLock(filePath);
            }
        }
	}

}
