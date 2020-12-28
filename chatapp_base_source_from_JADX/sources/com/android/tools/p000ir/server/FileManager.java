package com.android.tools.p000ir.server;

import android.util.Log;
import com.android.tools.p000ir.runtime.Paths;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/* renamed from: com.android.tools.ir.server.FileManager */
public class FileManager {
    public static final String CLASSES_DEX_SUFFIX = ".dex";
    private static final String FILE_NAME_ACTIVE = "active";
    private static final String FOLDER_NAME_LEFT = "left";
    private static final String FOLDER_NAME_RIGHT = "right";
    private static final String RELOAD_DEX_PREFIX = "reload";
    private static final String RESOURCE_FILE_NAME = "resources.ap_";
    private static final String RESOURCE_FOLDER_NAME = "resources";
    static final boolean USE_EXTRACTED_RESOURCES = false;
    private static boolean havePurgedTempDexFolder;

    private static File getDataFolder() {
        return new File(Paths.getDataDirectory(AppInfo.applicationId));
    }

    private static File getResourceFile(File base) {
        return new File(base, "resources.ap_");
    }

    private static File getTempDexFileFolder(File base) {
        return new File(base, "dex-temp");
    }

    public static File getNativeLibraryFolder() {
        return new File(Paths.getMainApkDataDirectory(AppInfo.applicationId), "lib");
    }

    public static File getReadFolder() {
        return new File(getDataFolder(), leftIsActive() ? FOLDER_NAME_LEFT : FOLDER_NAME_RIGHT);
    }

    public static void swapFolders() {
        setLeftActive(!leftIsActive());
    }

    public static File getWriteFolder(boolean wipe) {
        File folder = new File(getDataFolder(), leftIsActive() ? FOLDER_NAME_RIGHT : FOLDER_NAME_LEFT);
        if (wipe && folder.exists()) {
            delete(folder);
            if (!folder.mkdirs()) {
                Log.e(Logging.LOG_TAG, "Failed to create folder " + folder);
            }
        }
        return folder;
    }

    private static void delete(File file) {
        File[] files;
        if (file.isDirectory() && (files = file.listFiles()) != null) {
            for (File child : files) {
                delete(child);
            }
        }
        if (!file.delete()) {
            Log.e(Logging.LOG_TAG, "Failed to delete file " + file);
        }
    }

    private static boolean leftIsActive() {
        BufferedReader reader;
        File pointer = new File(getDataFolder(), FILE_NAME_ACTIVE);
        if (!pointer.exists()) {
            return true;
        }
        try {
            reader = new BufferedReader(new FileReader(pointer));
            boolean equals = FOLDER_NAME_LEFT.equals(reader.readLine());
            reader.close();
            return equals;
        } catch (IOException e) {
            return true;
        } catch (Throwable th) {
            reader.close();
            throw th;
        }
    }

    private static void setLeftActive(boolean active) {
        Writer writer;
        File folder = getDataFolder();
        File pointer = new File(folder, FILE_NAME_ACTIVE);
        if (pointer.exists()) {
            if (!pointer.delete()) {
                Log.e(Logging.LOG_TAG, "Failed to delete file " + pointer);
            }
        } else if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e(Logging.LOG_TAG, "Failed to create directory " + folder);
                return;
            }
            return;
        }
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pointer), "UTF-8"));
            writer.write(active ? FOLDER_NAME_LEFT : FOLDER_NAME_RIGHT);
            writer.close();
        } catch (IOException e) {
        } catch (Throwable th) {
            writer.close();
            throw th;
        }
    }

    public static File getExternalResourceFile() {
        File file = getResourceFile(getReadFolder());
        if (file.exists()) {
            return file;
        }
        if (!Log.isLoggable(Logging.LOG_TAG, 2)) {
            return null;
        }
        Log.v(Logging.LOG_TAG, "Cannot find external resources, not patching them in");
        return null;
    }

    public static File getTempDexFile() {
        File dataFolder = getDataFolder();
        File dexFolder = getTempDexFileFolder(dataFolder);
        if (!dexFolder.exists()) {
            if (!dexFolder.mkdirs()) {
                Log.e(Logging.LOG_TAG, "Failed to create directory " + dexFolder);
                return null;
            }
            havePurgedTempDexFolder = true;
        } else if (!havePurgedTempDexFolder) {
            purgeTempDexFiles(dataFolder);
        }
        File[] files = dexFolder.listFiles();
        int max = -1;
        if (files != null) {
            int max2 = -1;
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(RELOAD_DEX_PREFIX) && name.endsWith(CLASSES_DEX_SUFFIX)) {
                    try {
                        int version = Integer.decode(name.substring(RELOAD_DEX_PREFIX.length(), name.length() - CLASSES_DEX_SUFFIX.length())).intValue();
                        if (version > max2) {
                            max2 = version;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            max = max2;
        }
        File file2 = new File(dexFolder, String.format("%s0x%04x%s", new Object[]{RELOAD_DEX_PREFIX, Integer.valueOf(max + 1), CLASSES_DEX_SUFFIX}));
        if (Log.isLoggable(Logging.LOG_TAG, 2)) {
            Log.v(Logging.LOG_TAG, "Writing new dex file: " + file2);
        }
        return file2;
    }

    public static boolean writeRawBytes(File destination, byte[] bytes) {
        BufferedOutputStream output;
        try {
            output = new BufferedOutputStream(new FileOutputStream(destination));
            output.write(bytes);
            output.flush();
            output.close();
            return true;
        } catch (IOException ioe) {
            Log.wtf(Logging.LOG_TAG, "Failed to write file, clean project and rebuild " + destination, ioe);
            throw new RuntimeException(String.format("InstantRun could not write file %1$s, clean project and rebuild ", new Object[]{destination}));
        } catch (Throwable th) {
            output.close();
            throw th;
        }
    }

    public static boolean extractZip(File destination, byte[] zipBytes) {
        Log.wtf(Logging.LOG_TAG, "");
        return USE_EXTRACTED_RESOURCES;
    }

    public static void startUpdate() {
        getWriteFolder(true);
    }

    public static void finishUpdate(boolean wroteResources) {
        if (wroteResources) {
            swapFolders();
        }
    }

    public static void writeAaptResources(String relativePath, byte[] bytes) {
        File file = getResourceFile(getWriteFolder(USE_EXTRACTED_RESOURCES));
        File folder = file.getParentFile();
        if (folder.isDirectory() || folder.mkdirs()) {
            if (relativePath.equals("resources.ap_")) {
                writeRawBytes(file, bytes);
            } else {
                writeRawBytes(file, bytes);
            }
        } else if (Log.isLoggable(Logging.LOG_TAG, 2)) {
            Log.v(Logging.LOG_TAG, "Cannot create local resource file directory " + folder);
        }
    }

    public static String writeTempDexFile(byte[] bytes) {
        File file = getTempDexFile();
        if (file != null) {
            writeRawBytes(file, bytes);
            return file.getPath();
        }
        Log.e(Logging.LOG_TAG, "No file to write temp dex content to");
        return null;
    }

    public static void purgeTempDexFiles(File dataFolder) {
        File[] files;
        havePurgedTempDexFolder = true;
        File dexFolder = getTempDexFileFolder(dataFolder);
        if (dexFolder.isDirectory() && (files = dexFolder.listFiles()) != null) {
            for (File file : files) {
                if (file.getPath().endsWith(CLASSES_DEX_SUFFIX) && !file.delete()) {
                    Log.e(Logging.LOG_TAG, "Could not delete temp dex file " + file);
                }
            }
        }
    }

    public static long getFileSize(String path) {
        return -1;
    }

    public static byte[] getCheckSum(String path) {
        return null;
    }

    public static byte[] getCheckSum(File file) {
        return null;
    }
}
