package de.hirola.kintojava.logger;

import de.hirola.kintojava.Global;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
    private final KintoLoggerConfiguration configuration;
    private final boolean localLoggingEnabled;
    private Path logFilePath;

    /**
     * Get the logger instance to log local and remote.
     *
     * @param configuration of logger
     * @return The instance of logger object.
     */
    public static KintoLogger getInstance(@NotNull KintoLoggerConfiguration configuration) {
        if (instance == null) {
            instance = new KintoLogger(configuration);
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

        switch (configuration.getLogggingDestination()) {
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE:
                logToConsole(buildLogString(severity, tag, message, exception));
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.FILE:
                if (localLoggingEnabled) {
                    logToFile(buildLogString(severity, tag, message, exception));
                }
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToRemote(buildLogString(severity, tag, message, exception));
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.FILE:
                logToConsole(buildLogString(severity, tag, message, exception));
                logToFile(buildLogString(severity, tag, message, exception));
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToConsole(buildLogString(severity, tag, message, exception));
                logToRemote(buildLogString(severity, tag, message, exception));
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.FILE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToFile(buildLogString(severity, tag, message, exception));
                logToRemote(buildLogString(severity, tag, message, exception));
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.FILE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToConsole(buildLogString(severity, tag, message, exception));
                logToFile(buildLogString(severity, tag, message, exception));
                logToRemote(buildLogString(severity, tag, message, exception));
                break;
        }
    }

    /**
     * Sends a feedback for the app.
     * Not implemented yet.
     *
     * @param type: tye of feedback
     * @param feedback: content of the feedback
     */
    public void feedback(int type, String feedback) {
        System.out.println("Not implemented yet.");
    }

    private KintoLogger(KintoLoggerConfiguration configuration) {
        this.configuration = configuration;
        String appPackageName = configuration.getAppPackageName();
        String logfileName;
        if (appPackageName.contains(".")) {
            logfileName = appPackageName.substring(appPackageName.lastIndexOf(".") + 1);
        } else {
            logfileName = appPackageName;
        }
        // build the path, determine if android or jvm
        // see https://developer.android.com/reference/java/lang/System#getProperties()
        try {
            if (System.getProperty("java.vm.vendor").equals("The Android Project")) {
                // Android
                // path for local database on Android
                logFilePath = Paths.get("/data/data/" + logfileName + "/.log");
            } else {
                // JVM
                //  path for local database on JVM
                String userHomeDir = System.getProperty("user.home");
                logFilePath = Paths.get(userHomeDir + File.separator + ".kinto-java" + File.separator + logfileName + ".log");
            }
        } catch (Exception exception){
            logFilePath = null;
        }
        if (!initFileLogging()) {
            System.out.println("Logging to file is disable! Activate debug mode for more information.");
            localLoggingEnabled = false;
        } else {
            localLoggingEnabled = true;
                logToFile(buildLogString(INFO, TAG, "Logging to file enabled. Start logging now ...", null));
        }
    }

    //  simple print to console
    private void logToConsole(String entry) {
        System.out.println(entry);
    }

    //  logging to file
    private void logToFile(String entry) {
        if (localLoggingEnabled) {
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
                                        LocalDate localDate = LocalDate.now();
                                        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                                        String archiveLogFileName = this.logFilePath.getFileName().toString() + formatter.format(localDate);
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
        String entryString = formatter.format(timeStamp);
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
