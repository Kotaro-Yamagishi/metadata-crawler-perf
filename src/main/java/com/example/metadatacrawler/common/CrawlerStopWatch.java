package com.example.metadatacrawler.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

public class CrawlerStopWatch {

    private final StopWatch stopWatch = new StopWatch();
    private final Logger log;

    public CrawlerStopWatch(Class<?> caller) {
        this.log = LoggerFactory.getLogger(caller);
    }

    public void start(String taskName) {
        stopWatch.start(taskName);
    }

    public void stop() {
        stopWatch.stop();
        StopWatch.TaskInfo last = stopWatch.getLastTaskInfo();
        log.info("[stopwatch] task='{}' elapsed={}ms", last.getTaskName(), last.getTimeMillis());
    }

    public long totalMillis() {
        return stopWatch.getTotalTimeMillis();
    }

    public String prettyPrint() {
        return stopWatch.prettyPrint();
    }
}
