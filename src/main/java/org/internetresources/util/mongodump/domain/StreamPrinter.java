package org.internetresources.util.mongodump.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

/**
 * print a stream to the stdout and forward each line to a spylogs instance
 *
 */
@Slf4j
public class StreamPrinter extends Thread {
    private final InputStream inputStream;
    private final String streamName;
	private final boolean isErrorStream;
    private SpyLogs spyLogs = null;

    public StreamPrinter(String streamName, InputStream inputStream, SpyLogs spy) {
		this.streamName = streamName;
        this.inputStream = inputStream;
        this.isErrorStream = false;
        setSpy(spy);
    }

    public StreamPrinter(String streamName, InputStream inputStream, SpyLogs spy, boolean errorStream) {
		this.streamName = streamName;
        this.inputStream = inputStream;
        this.isErrorStream = errorStream;
        setSpy(spy);
    }

    private BufferedReader getBufferedReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
        BufferedReader br = getBufferedReader(inputStream);
        String ligne = "";
        try {
            ligne = br.readLine();
            while (ligne != null) {
                String logLine = String.format("%s %s", streamName, ligne);
                if (isErrorStream) {
                	log.error(logLine);
                } else {
                	log.debug(logLine);
                }
                if (spyLogs != null) {
                    spyLogs.spy(logLine);
                }
                ligne = br.readLine();
            }
        } catch (IOException e) {
            String excMsg = e.getMessage();
            if (excMsg != null && excMsg.contains("Stream closed")) {
                return;
            }
            e.printStackTrace();
        }
    }

    public void setSpy(SpyLogs spyLogs) {
        this.spyLogs = spyLogs;
    }
}
