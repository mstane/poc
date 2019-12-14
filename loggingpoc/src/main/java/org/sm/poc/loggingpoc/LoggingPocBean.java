package org.sm.poc.loggingpoc;

public class LoggingPocBean {
    private final long id;
    private final String content;

    public LoggingPocBean(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
