package com.github.boly38.mongodump.domain.logger;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * add spy for logs 
 * for a given spy, record the latest seen log line
 */
@Slf4j
public class SpyLogs {
    private Map<Integer, String> spyLogs = new HashMap<Integer, String>();
    private Map<Integer, String> spyRecorderdLogs = new HashMap<Integer, String>();
    private static int spyLogsId=0;

    public int addSpy(String spyLog) {
        spyLogs.put(spyLogsId,spyLog);
        int spyId = spyLogsId++;
		return spyId;
    }

    /**
     * if the line is spyed, then we remove the spyLogs
     * @param line
     */
    public void spy(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        Map<Integer, String> resultLogs = new HashMap<Integer, String>();
        for(Integer spyLogId: spyLogs.keySet()) {
            String spyLog = spyLogs.get(spyLogId);
            if (!line.contains(spyLog)) {
                resultLogs.put(spyLogId, spyLog);
                continue;
            }
            log.debug("log spy have seen '{}' ==> {}", spyLog, line);
            spyRecorderdLogs.put(spyLogId, line);
        }
        spyLogs = resultLogs;
    }

    public boolean hasSpy(int spyId) {
        return spyLogs.containsKey(spyId);
    }

    public String getSpy(int spyId) {
        return spyLogs.get(spyId);
    } 

    public String getRecorderdSpy(int spyId) {
        return spyRecorderdLogs.get(spyId);
    } 

    public int size() {
        return spyLogs.size();
    }

}
