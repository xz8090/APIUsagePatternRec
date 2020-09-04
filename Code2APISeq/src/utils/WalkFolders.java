package utils;

import java.io.File;

public class WalkFolders {

	public static void folderMethod(String path) {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                    	String folderName = file2.getName();
                        System.out.println("文件夹:" + folderName);
                        if(!folderName.startsWith("."))
                        	folderMethod(file2.getAbsolutePath());
                    } else {
                    	String fileName = file2.getName();
                        System.out.println("文件名:" + fileName);
                        if(fileName.endsWith(".java")) {
                        	String filePath = file2.getAbsolutePath();
                        }
                        	
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fileUrl = "C:\\Users\\Administrator\\Downloads\\java-med\\java-med\\training\\orhanobut__hawk";
		folderMethod(fileUrl);
	}

}
