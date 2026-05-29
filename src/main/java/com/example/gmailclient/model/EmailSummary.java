package com.example.gmailclient.model;

public class EmailSummary {

    private String id;
    private String threadId;
    private String from;
    private String subject;
    private String date;
    private String snippet;

    public EmailSummary() {}

    public EmailSummary(String id, String threadId, String from, String subject, String date, String snippet) {
        this.id = id;
        this.threadId = threadId;
        this.from = from;
        this.subject = subject;
        this.date = date;
        this.snippet = snippet;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
}
