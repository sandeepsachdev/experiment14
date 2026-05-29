package com.example.gmailclient.model;

public class EmailMessage {

    private String id;
    private String threadId;
    private String from;
    private String to;
    private String subject;
    private String date;
    private String bodyHtml;
    private String bodyText;
    /** The Message-ID header value, used for reply threading (In-Reply-To / References). */
    private String messageId;
    private String references;

    public EmailMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }

    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getReferences() { return references; }
    public void setReferences(String references) { this.references = references; }
}
