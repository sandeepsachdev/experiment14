package com.example.gmailclient.mailing.service;

import com.example.gmailclient.mailing.config.MailingProperties;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InboxPollingService {

    private static final Logger log = LoggerFactory.getLogger(InboxPollingService.class);

    private static final Pattern MESSAGE_ID_PATTERN =
            Pattern.compile("<[^>]+\\.campaign\\d+@[^>]+>", Pattern.CASE_INSENSITIVE);

    private final MailingProperties props;
    private final CampaignService campaignService;

    private final AtomicInteger lastProcessedMessageNum = new AtomicInteger(0);

    public InboxPollingService(MailingProperties props, CampaignService campaignService) {
        this.props = props;
        this.campaignService = campaignService;
    }

    @Scheduled(fixedDelayString = "${mailing.poll.interval-ms:60000}")
    public void pollInbox() {
        MailingProperties.Imap imapConfig = props.getImap();
        if (imapConfig.getUsername() == null || imapConfig.getUsername().startsWith("your-")) {
            log.debug("IMAP credentials not configured, skipping inbox poll");
            return;
        }

        Store store = null;
        Folder folder = null;
        try {
            Properties mailProps = new Properties();
            mailProps.put("mail.store.protocol", "imaps");
            mailProps.put("mail.imaps.host", imapConfig.getHost());
            mailProps.put("mail.imaps.port", String.valueOf(imapConfig.getPort()));
            mailProps.put("mail.imaps.ssl.enable", "true");
            mailProps.put("mail.imaps.connectiontimeout", "10000");
            mailProps.put("mail.imaps.timeout", "10000");

            Session session = Session.getInstance(mailProps);
            store = session.getStore("imaps");
            store.connect(imapConfig.getHost(), imapConfig.getUsername(), imapConfig.getPassword());

            folder = store.getFolder(imapConfig.getFolder());
            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) return;

            // Process only new messages since last poll
            int startFrom = lastProcessedMessageNum.get() + 1;
            if (startFrom > totalMessages) return;

            Message[] messages = folder.getMessages(startFrom, totalMessages);
            log.debug("Polling inbox: processing {} new messages (#{} to #{})", messages.length, startFrom, totalMessages);

            for (Message msg : messages) {
                try {
                    processMessage(msg);
                } catch (Exception e) {
                    log.warn("Error processing message: {}", e.getMessage());
                }
            }

            lastProcessedMessageNum.set(totalMessages);

        } catch (Exception e) {
            log.error("Inbox polling failed: {}", e.getMessage());
        } finally {
            closeQuietly(folder, store);
        }
    }

    private void processMessage(Message msg) throws MessagingException, IOException {
        String from = getFrom(msg);
        String subject = msg.getSubject() != null ? msg.getSubject() : "";

        // Check bounce: MAILER-DAEMON, postmaster, null sender, or delivery status notification subjects
        if (isBounceMessage(from, subject)) {
            String originalMessageId = extractOriginalMessageId(msg);
            if (originalMessageId != null) {
                String reason = extractBounceReason(msg);
                LocalDateTime at = getMessageDate(msg);
                log.info("Bounce detected for messageId={}, reason={}", originalMessageId, reason);
                campaignService.markBounced(originalMessageId, reason, at);
            }
            return;
        }

        // Check reply: In-Reply-To or References header matches a campaign Message-ID
        String replyToId = extractReplyOriginalMessageId(msg);
        if (replyToId != null) {
            String snippet = extractTextBody(msg);
            if (snippet != null && snippet.length() > 500) {
                snippet = snippet.substring(0, 500);
            }
            LocalDateTime at = getMessageDate(msg);
            log.info("Reply detected for messageId={}", replyToId);
            campaignService.markReplied(replyToId, snippet, at);
        }
    }

    private boolean isBounceMessage(String from, String subject) {
        if (from == null) return true;
        String fromLower = from.toLowerCase();
        if (fromLower.contains("mailer-daemon") || fromLower.contains("postmaster") ||
                fromLower.contains("mail delivery") || fromLower.contains("delivery subsystem")) {
            return true;
        }
        String subjectLower = subject.toLowerCase();
        return subjectLower.contains("undelivered mail") ||
                subjectLower.contains("delivery status notification") ||
                subjectLower.contains("delivery failure") ||
                subjectLower.contains("returned mail") ||
                subjectLower.contains("mail delivery failed") ||
                subjectLower.contains("failure notice");
    }

    private String extractOriginalMessageId(Message msg) throws MessagingException, IOException {
        // Try In-Reply-To and References headers first (some MTAs set these on DSNs)
        String[] inReplyTo = msg.getHeader("In-Reply-To");
        if (inReplyTo != null) {
            for (String h : inReplyTo) {
                if (looksLikeCampaignMessageId(h)) return h.trim();
            }
        }

        // Try to find Message-ID in the body (embedded original message)
        String body = extractTextBody(msg);
        if (body != null) {
            Matcher m = MESSAGE_ID_PATTERN.matcher(body);
            if (m.find()) return m.group();
        }

        // Try to parse message/delivery-status or message/rfc822 parts
        if (msg.isMimeType("multipart/*") && msg.getContent() instanceof MimeMultipart mp) {
            return extractMessageIdFromMultipart(mp);
        }

        return null;
    }

    private String extractMessageIdFromMultipart(MimeMultipart mp) throws MessagingException, IOException {
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            if (part.isMimeType("message/rfc822") || part.isMimeType("message/delivery-status")) {
                Object content = part.getContent();
                if (content instanceof Message embeddedMsg) {
                    String[] msgIdHeaders = embeddedMsg.getHeader("Message-ID");
                    if (msgIdHeaders != null) {
                        for (String h : msgIdHeaders) {
                            if (looksLikeCampaignMessageId(h)) return h.trim();
                        }
                    }
                }
                // Parse delivery-status text for Original-Message-ID
                if (part.isMimeType("message/delivery-status")) {
                    String dsBody = (String) part.getContent();
                    Matcher m = MESSAGE_ID_PATTERN.matcher(dsBody);
                    if (m.find()) return m.group();
                }
            } else if (part.isMimeType("multipart/*")) {
                String found = extractMessageIdFromMultipart((MimeMultipart) part.getContent());
                if (found != null) return found;
            }
        }
        return null;
    }

    private String extractReplyOriginalMessageId(Message msg) throws MessagingException {
        String[] inReplyTo = msg.getHeader("In-Reply-To");
        if (inReplyTo != null) {
            for (String h : inReplyTo) {
                if (looksLikeCampaignMessageId(h)) return h.trim();
            }
        }
        String[] references = msg.getHeader("References");
        if (references != null) {
            for (String h : references) {
                Matcher m = MESSAGE_ID_PATTERN.matcher(h);
                if (m.find()) return m.group();
            }
        }
        return null;
    }

    private boolean looksLikeCampaignMessageId(String id) {
        return id != null && MESSAGE_ID_PATTERN.matcher(id.trim()).matches();
    }

    private String extractBounceReason(Message msg) {
        try {
            if (msg.isMimeType("multipart/*")) {
                MimeMultipart mp = (MimeMultipart) msg.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        String text = (String) part.getContent();
                        // Return first 300 chars as bounce reason
                        return text.length() > 300 ? text.substring(0, 300) : text;
                    }
                }
            }
            String body = extractTextBody(msg);
            if (body != null) return body.length() > 300 ? body.substring(0, 300) : body;
        } catch (Exception e) {
            log.debug("Could not extract bounce reason: {}", e.getMessage());
        }
        return "Delivery failure";
    }

    private String extractTextBody(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            MimeMultipart mp = (MimeMultipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String result = extractTextBody(mp.getBodyPart(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private String getFrom(Message msg) {
        try {
            Address[] from = msg.getFrom();
            if (from != null && from.length > 0) return from[0].toString();
        } catch (MessagingException e) {
            log.debug("Could not get from address: {}", e.getMessage());
        }
        return null;
    }

    private LocalDateTime getMessageDate(Message msg) {
        try {
            Date date = msg.getReceivedDate();
            if (date == null) date = msg.getSentDate();
            if (date != null) return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (MessagingException e) {
            log.debug("Could not get message date: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    private void closeQuietly(Folder folder, Store store) {
        if (folder != null && folder.isOpen()) {
            try { folder.close(false); } catch (MessagingException ignored) {}
        }
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (MessagingException ignored) {}
        }
    }
}
