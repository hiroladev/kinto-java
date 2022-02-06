package de.hirola.kintojava;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
 public class KintoLogger {

    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;
    public static final int DEBUG = 3;
    public static final int BUG = 4;
    public static final int FEATURE_REQUEST = 5;

    private static final String TAG = KintoLogger.class.getSimpleName();

    private static KintoLogger instance;
    private boolean isLogToFileEnabled;
    private Path logFilePath;

    public static KintoLogger getInstance(@Nullable String logFileName) {
        if (instance == null) {
            instance = new KintoLogger(logFileName);
        }
        return instance;
    }

    /**
     * Log a message to different destinations.
     * Timestamps are in UTC and ISO format.
     *
     * @param severity: severity of the log message
     * @param tag of the log source
     * @param exception of the log cause
     * @param  message: message to log
     */
    public void log(int severity, @Nullable String tag, @NotNull String message, @Nullable Exception exception) {
        if (Global.DEBUG || severity == KintoLogger.DEBUG) {
            logToConsole(buildLogString(severity, tag, message, exception));
        }
        logToFile(buildLogString(severity, tag, message, exception));
    }

    /**
     The "struct" of  possible destinations for logging.
     You can combine for multiple targets, e.g. 3 means
     log to console and file.
     For remote logging must be specified a valid log server.
     */
    public static class LOGGING_DESTINATION {
        /**
         *  A normal feedback.
         */
        public static final int CONSOLE = 1;
        /**
         * A Feedback for an app issue.
         */
        public static final int FILE = 3;
        /**
         A Feedback for a new feature.
         */
        public static final int REMOTE = 5;
    }

    private KintoLogger(String logFileName) {
        String logfile = Objects.requireNonNullElse(logFileName, "kinto-java");
        // build the path, determine if android or jvm
        // see https://developer.android.com/reference/java/lang/System#getProperties()
        try {
            String vendor = System.getProperty("java.vm.vendor"); // can be null
            if (vendor != null) {
                if (vendor.equals("The Android Project")) {
                    // Android
                    // path for local database on Android
                    logFilePath = Paths.get("/data/data/" + logfile + "/.log");
                } else {
                    // JVM
                    //  path for local database on JVM
                    String userHomeDir = System.getProperty("user.home"); // can be null
                    if (userHomeDir != null) {
                        logFilePath = Paths.get(userHomeDir + File.separator + ".kinto-java" + File.separator + logfile + ".log");
                    } else {
                        logFilePath = null;
                    }
                }
            } else {
                throw new KintoException("Could not determine the runtime environment.");
            }
        } catch (Exception exception){
            isLogToFileEnabled = false;
            System.out.println("Logging to file is disable.");
        }
        if (isLogToFileEnabled) {
            if (!initFileLogging()) {
                System.out.println("Logging to file is disable.");
                isLogToFileEnabled = false;
            } else {
                isLogToFileEnabled = true;
                logToFile(buildLogString(INFO, TAG, "Logging to file enabled. Start logging now ...", null));
            }
        }
    }

    //  simple print to console
    private void logToConsole(String entry) {
        System.out.println(entry);
    }

    //  logging to file
    private void logToFile(String entry) {
        if (isLogToFileEnabled) {
            try {
                if (Files.isWritable(logFilePath)) {
                    Files.write(logFilePath, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                }
            } catch (IOException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
            }
        }
    }

    // remote logging
    private void logToRemote(String entry) {
        System.out.println("Sorry. Remote logging not available yet:"
                + entry);
    }

    // check file system permissions, create subdirectories and log file, ...
    private boolean initFileLogging() {
        if (logFilePath == null) {
            return false;
        }
        // max file size in byte
        int maxFileSize = 1000000;
        // max count of log files
        int maxFileCount = 10;
        // create new log file?
        boolean createLogFile = false;
        // check the file system
        // 1. log file path exists?
        if (Files.exists(logFilePath)) {
            // 2. check if is a dir
            if (!Files.isDirectory(logFilePath)) {
                // 3. is a regular file?
                if (Files.isRegularFile(logFilePath)) {
                    // 4. can we write into the file?
                    if (Files.isWritable(logFilePath)) {
                        // 5. check size and count of log file(s)
                        try {
                            // check the size and rollover
                            if (Files.size(logFilePath) >= maxFileSize) {
                                // determine the count of log files
                                // and delete the oldest log file
                                try {
                                    int fileCount = 0;
                                    FileTime lastTimestamp = FileTime.fromMillis(0);
                                    //  the oldest log file
                                    Path oldestLogFilePath = null;
                                    DirectoryStream<Path> files = Files.newDirectoryStream(logFilePath);

                                    for(Path file: files) {
                                        if(Files.isRegularFile(file)) {
                                            if (file.startsWith(logFilePath.getFileName())) {
                                                fileCount++;
                                                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                                                FileTime creationTime = attributes.creationTime();
                                                // compare creation times
                                                if(creationTime.compareTo(lastTimestamp) < 0) {
                                                    //  notice the oldest log file
                                                    oldestLogFilePath = file;
                                                    lastTimestamp = creationTime;
                                                }
                                            }
                                        }
                                    }
                                    // Closes this stream and releases any system resources associated with it.
                                    try {
                                        files.close();
                                    } catch (IOException exception) {
                                        if (Global.DEBUG) {
                                            exception.printStackTrace();
                                        }
                                    }
                                    if (fileCount >= maxFileCount) {
                                        // delete the oldest log file
                                        if (oldestLogFilePath != null) {
                                            Files.delete(oldestLogFilePath);
                                        }
                                        // archive the last log file
                                        Instant timeStamp = Instant.now().atZone(ZoneOffset.UTC).toInstant();
                                        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                                        String archiveLogFileName = this.logFilePath.getFileName().toString() + formatter.format(timeStamp);
                                        Path archivePath = Paths.get(logFilePath.getParent().toString(), archiveLogFileName);
                                        // if the new file exists, we write in old log file and try to rename later
                                        if (!Files.exists(archivePath)) {
                                            Files.move(logFilePath,archivePath);
                                            createLogFile = true;
                                        }
                                    }
                                    // we can create a new log file
                                    createLogFile = true;
                                } catch (IOException exception) {
                                    // if an operation failed, we write in old log file and try to rename later
                                    if (Global.DEBUG) {
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        } catch (IOException exception) {
                            // other file system errors -> no file logging available
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                                return false;
                            }
                        }
                    } else {
                        if (Global.DEBUG) {
                            System.out.println(logFilePath + " is not writeable! Disable logging to file.");
                        }
                        return false;
                    }
                } else {
                    if (Global.DEBUG) {
                        System.out.println(logFilePath + " is not a regular file! Disable logging to file.");
                    }
                    return false;
                }
            } else {
                // is a directory
                if (Global.DEBUG) {
                    System.out.println(logFilePath + " is a directory! Disable logging to file.");
                }
                return false;
            }
        } else {
            // create dir(s)
            File logFileFolderStructure = new File(logFilePath.getParent().toString());
            try {
                if (logFileFolderStructure.mkdirs()) {
                    createLogFile = true;
                }
            } catch (SecurityException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
                return false;
            }
        }
        // create log file
        if (createLogFile) {
            try {
                Files.createFile(logFilePath);
            } catch (IOException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
                return false;
            }
        }
        return true;
    }

    private String buildLogString(int severity, @Nullable String tag, @NotNull String message, @Nullable Exception exception) {
        // timestamp in UTC
        Instant timeStamp = Instant.now().atZone(ZoneOffset.UTC).toInstant();
        // timestamp in ISO format
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String entryString = formatter.format(timeStamp) + " - " + tag + " - ";
        switch (severity) {
            case INFO:
                entryString += " - " + "Info: ";
                break;
            case WARNING:
                entryString += " - " + "Warning: ";
                break;
            case ERROR:
                entryString += " - " + "Error: ";
                break;
            case DEBUG:
                entryString += " - " + "Debug: ";
                break;
            default :
                entryString += " - " + "Unknown: ";
        }
        entryString += message + "\n";
        if (exception != null) {
            entryString += exception.getMessage() + "\n";
        }
        return entryString;
    }

}
