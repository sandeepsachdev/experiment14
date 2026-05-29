package com.example.gmailclient.mailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mailing")
public class MailingProperties {

    private Smtp smtp = new Smtp();
    private Imap imap = new Imap();
    private Poll poll = new Poll();
    private String messageIdDomain = "mailing.local";

    public Smtp getSmtp() { return smtp; }
    public void setSmtp(Smtp smtp) { this.smtp = smtp; }
    public Imap getImap() { return imap; }
    public void setImap(Imap imap) { this.imap = imap; }
    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }
    public String getMessageIdDomain() { return messageIdDomain; }
    public void setMessageIdDomain(String messageIdDomain) { this.messageIdDomain = messageIdDomain; }

    public static class Smtp {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username;
        private String password;
        private String from;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFrom() { return from != null ? from : username; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class Imap {
        private String host = "imap.gmail.com";
        private int port = 993;
        private String username;
        private String password;
        private String folder = "INBOX";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
    }

    public static class Poll {
        private long intervalMs = 60000;

        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }
}
