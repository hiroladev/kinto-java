package de.hirola.kintojava.logger;

import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import de.hirola.kintojava.Global;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class KintoLogger {

    private static KintoLogger instance;
    private final KintoLoggerConfiguration configuration;
    private final boolean localLoggingEnabled;
    private final Path logFilePath;
    private boolean remoteLoggingEnabled;

    public static KintoLogger getInstance(KintoLoggerConfiguration configuration) {
        if (instance == null) {
            instance = new KintoLogger(configuration);
        }
        return instance;
    }

    /**
     * Logging message to different destinations.
     *
     * @param severity: severity of the log message
     * @param  message: message to log
     * @see LogEntry
     */
    public void log(int severity, String message) {

        LogEntry entry = new LogEntry(severity, message);
        // determine the log destination
        switch (configuration.getLogggingDestination()) {
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE:
                logToConsole(entry);
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.FILE:
                if (localLoggingEnabled) {
                    logToFile(entry);
                }
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToRemote(entry);
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.FILE:
                logToConsole(entry);
                logToFile(entry);
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToConsole(entry);
                logToRemote(entry);
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.FILE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToFile(entry);
                logToRemote(entry);
                break;
            case KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.FILE +
                    KintoLoggerConfiguration.LOGGING_DESTINATION.REMOTE:
                logToConsole(entry);
                logToFile(entry);
                logToRemote(entry);
                break;
        }
    }

    /**
     * Sends a feedback for the app.
     *
     * @param type: tye of feedback
     * @param feedback: content of the feedback
     * @see FeedbackEntry
     */
    public void feedback(int type, String feedback) {

       FeedbackEntry entry = new FeedbackEntry(type, feedback);

    }

    private KintoLogger(KintoLoggerConfiguration configuration) {
        this.configuration = configuration;
        this.logFilePath = Paths.get(configuration.getLocalLogPath(), "kinto-java.log");
        if (!initFileLogging()) {
            System.out.println("Logging to file is disable! Activate debug mode for more information.");
            localLoggingEnabled = false;
        } else {
            localLoggingEnabled = true;
            logToFile(new LogEntry(LogEntry.Severity.INFO,"File-Logging started ..."));
        }
    }

    //  simple print to console
    private void logToConsole(LogEntry entry) {
        System.out.println(buildLogEntryString(entry));
    }

    //  logging to file
    private void logToFile(LogEntry entry) {
        if (localLoggingEnabled) {
            try {
                if (Files.isWritable(logFilePath)) {
                    Files.write(logFilePath,
                            buildLogEntryString(entry).getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.APPEND);
                }
            } catch (IOException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
            }
        }
    }

    // remote logging
    private void logToRemote(LogEntry entry) {
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
        Path logDirPath = Paths.get(this.configuration.getLocalLogPath());
        // check the file system
        // 1. log dir path exists?
        if (Files.exists(logDirPath)) {
            // 2. check is a dir
            if (Files.isDirectory(logDirPath)) {
                // 3. can we write into the dir?
                if (Files.isWritable(logDirPath)) {
                    // 4. does the log file exists?
                    if (Files.exists(logFilePath)) {
                        // 5. check is a file?
                        if (Files.isRegularFile(logFilePath)) {
                            // 6. can we write into the file?
                            if (Files.isWritable(logFilePath)) {
                                // 7. check size and count of log file(s)
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
                                            DirectoryStream<Path> files = Files.newDirectoryStream(logDirPath);

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
                                System.out.println(logFilePath + " is a dir! Disable logging to file.");
                            }
                            return false;
                        }
                    } else  {
                        // we create a new log file
                        createLogFile = true;
                    }
                } else {
                    // directory not writeable
                    if (Global.DEBUG) {
                        System.out.println(logDirPath + " is not writeable! Disable logging to file.");
                    }
                    return false;
                }
            } else {
                // not a directory
                if (Global.DEBUG) {
                    System.out.println(logDirPath + " is a file! Disable logging to file.");
                }
                return false;
            }
        } else {
            // create dir(s)
            File logFileFolderStructure = new File(logDirPath.toString());
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

    private String buildLogEntryString(LogEntry entry) {
        Instant logDate = Instant.ofEpochMilli(entry.getTimeStamp());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String entryString = formatter.format(logDate);
        switch (entry.getSeverity()) {
            case LogEntry.Severity.INFO:
                entryString = entryString.concat(" - " + "Info: ");
                break;
            case LogEntry.Severity.WARNING:
                entryString = entryString.concat(" - " + "Warning: ");
                break;
            case LogEntry.Severity.ERROR:
                entryString = entryString.concat(" - " + "Error: ");
                break;
            case LogEntry.Severity.DEBUG:
                entryString = entryString.concat(" - " + "Debug: ");
                break;
            default :
                entryString = entryString.concat(" - " + "Unknown: ");
        }
        entryString = entryString.concat(entry.getMessage() + "\n");

        return entryString;
    }

}
