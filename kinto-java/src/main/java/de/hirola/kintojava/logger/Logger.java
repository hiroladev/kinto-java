package de.hirola.kintojava.logger;

import de.hirola.kintojava.Global;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

public class Logger {

    private static Logger instance;
    private final LoggerConfiguration configuration;
    private boolean localLoggingEnabled;
    private Path logFilePath;
    private boolean remoteLoggingEnabled;

    private Logger(LoggerConfiguration configuration) {
        this.configuration = configuration;
        this.logFilePath = Paths.get(configuration.getLocalLogPath(), "kinto-java.log");
        if (!initFileLogging()) {
            System.out.println("Logging to file is disable! Activate debug mode for more information.");
            this.localLoggingEnabled = false;
        } else {
            this.localLoggingEnabled = true;
        }
    }

    // check file system permissions, create subdirs and log file, ...
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
                                            if (Global.DEBUG) {
                                                exception.printStackTrace();
                                            }
                                        }
                                    }
                                } catch (IOException exception) {
                                    // all file system errors
                                    if (Global.DEBUG) {
                                        exception.printStackTrace();
                                        return false;
                                    }
                                }
                            } else {
                                if (Global.DEBUG) {
                                    System.out.println(logFilePath.toString() + " is not writeable! Disable logging to file.");
                                }
                                return false;
                            }
                        } else {
                            if (Global.DEBUG) {
                                System.out.println(logFilePath.toString() + " is a dir! Disable logging to file.");
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
            Iterator<Path> pathIterator = logDirPath.iterator();
            while(pathIterator.hasNext()) {
                Path pathElement = pathIterator.next();
                //  if sub dir not exist -> create it
                if (!Files.exists(pathElement)) {
                    Path parent = pathElement.getParent();
                    // is this a dir?
                    if (Files.isDirectory(parent)) {
                        // can we create a dir?
                        if (Files.isWritable(parent)) {
                            //  create sub dir
                            try {
                                Files.createDirectories(pathElement);
                                if (Global.DEBUG) {
                                    System.out.println("Directory " + pathElement + " created.");
                                }
                            } catch (IOException exception) {
                                if (Global.DEBUG) {
                                    System.out.println("Can't create the directory " + pathElement + " Disable logging to file.");
                                    exception.printStackTrace();
                                }
                                return false;
                            }
                        } else {
                            // not writeable
                            if (Global.DEBUG) {
                                System.out.println("Can't create the directory " + pathElement + " Disable logging to file.");
                            }
                            return false;
                        }
                    } else {
                        // parent path element is a file
                        if (Global.DEBUG) {
                            System.out.println(pathElement.toString() + " is not a dir. Disable logging to file.");
                        }
                        return false;
                    }

                }

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
        LocalDateTime logDate = Instant.ofEpochMilli(entry.getTimeStamp())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
        formatter.format(logDate);
        String entryString = "";
        entryString+= switch (entry.getSeverity()) {
            case LogEntry.Severity.INFO -> entryString + " - " + "Info: ";
            case LogEntry.Severity.WARNING -> entryString + " - " + "Warning: ";
            case LogEntry.Severity.ERROR -> entryString + " - " + "Error: ";
            case LogEntry.Severity.DEBUG -> entryString + " - " + "Debug: ";
            default -> entryString + " - " + "Unknown: ";
        };
        return entryString + entry.getMessage();

    }

    //  simple print to console
    private void out(LogEntry entry) {

        System.out.println(this.buildLogEntryString(entry));

    }

    //  logging to file
    private void logToFile(LogEntry entry) {

        if (this.localLoggingEnabled) {

        }

    }

    public static Logger getInstance(LoggerConfiguration configuration) {
        if (instance == null) {
            instance = new Logger(configuration);
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

            case LoggerConfiguration.LOGGING_DESTINATION.CONSOLE:
                this.out(entry);
            // first all out to console ...
            default:
                this.out(entry);
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
}
