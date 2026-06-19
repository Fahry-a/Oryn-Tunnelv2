package net.oryn.mc.orynTunnelv2.log;

import com.github.luben.zstd.ZstdOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class LogManager {

    private final Logger logger;
    private final File logsDir;
    private final File logFile;
    private PrintWriter logWriter;
    private long logMaxSize;
    private long currentSize;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ARCHIVE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public LogManager(File dataFolder, Logger logger) {
        this(dataFolder, logger, 10 * 1024 * 1024);
    }

    public LogManager(File dataFolder, Logger logger, long logMaxSize) {
        this.logger = logger;
        this.logsDir = new File(dataFolder, "logs");
        this.logFile = new File(logsDir, "log.txt");
        this.logMaxSize = logMaxSize;
        init();
    }

    private void init() {
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            logger.warning("Failed to create logs directory");
        }
        try {
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
            currentSize = logFile.exists() ? logFile.length() : 0;
        } catch (IOException e) {
            logger.severe("Failed to create log file: " + e.getMessage());
        }
    }

    public void setLogMaxSize(long maxBytes) {
        this.logMaxSize = maxBytes;
    }

    public synchronized void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String line = "[" + timestamp + "] " + message;
            logWriter.println(line);
            currentSize += line.getBytes().length + 1;

            if (logMaxSize > 0 && currentSize >= logMaxSize) {
                rotate();
            }
        }
    }

    private void rotate() {
        close();

        if (!logFile.exists() || logFile.length() == 0) {
            init();
            return;
        }

        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMAT);
        File archiveDir = new File(logsDir, "archive");
        if (!archiveDir.exists() && !archiveDir.mkdirs()) {
            logger.warning("Failed to create archive directory");
            init();
            return;
        }

        String archiveName = "cloudflared-" + timestamp + ".zst";
        File archiveFile = new File(archiveDir, archiveName);

        try (FileInputStream fis = new FileInputStream(logFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(archiveFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zstdOut = new ZstdOutputStream(bos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                zstdOut.write(buffer, 0, len);
            }

            if (!logFile.delete()) {
                logger.warning("Failed to delete log file after rotation");
            }

            logger.info("Log rotated: " + archiveName);

        } catch (IOException e) {
            logger.warning("Failed to rotate log: " + e.getMessage());
        }

        init();
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }

    public void archive() {
        close();

        if (!logFile.exists() || logFile.length() == 0) {
            return;
        }

        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMAT);
        File archiveDir = new File(logsDir, "archive");
        if (!archiveDir.exists() && !archiveDir.mkdirs()) {
            logger.warning("Failed to create archive directory");
            return;
        }

        String archiveName = "cloudflared-" + timestamp + ".zst";
        File archiveFile = new File(archiveDir, archiveName);

        try (FileInputStream fis = new FileInputStream(logFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(archiveFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zstdOut = new ZstdOutputStream(bos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                zstdOut.write(buffer, 0, len);
            }

            if (!logFile.delete()) {
                logger.warning("Failed to delete log file after archiving");
            }

            logger.info("Log archived: " + archiveName);

        } catch (IOException e) {
            logger.warning("Failed to archive log: " + e.getMessage());
        }

        init();
    }
}
