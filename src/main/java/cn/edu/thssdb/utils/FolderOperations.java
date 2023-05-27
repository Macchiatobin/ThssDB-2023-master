package cn.edu.thssdb.utils;

import java.io.File;

public class FolderOperations {
  public static void deleteFolder(File folder) { // Delete Folder and files recursively
    if (!folder.exists()) {
      return;
    }

    if (folder.isDirectory()) {
      File[] files = folder.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteFolder(file);
        }
      }
    }
    // Delete empty folder/file
    folder.delete();
  }
}
