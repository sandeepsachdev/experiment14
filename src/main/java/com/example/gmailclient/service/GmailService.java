package com.example.gmailclient.service;

import com.example.gmailclient.model.EmailMessage;
import com.example.gmailclient.model.EmailSummary;
import com.example.gmailclient.model.LabelInfo;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final String GMAIL_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").withZone(ZoneId.systemDefault());

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .baseUrl(GMAIL_BASE)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    // -------------------------------------------------------------------------
    // List messages for a label (returns summaries with full header data)
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<EmailSummary> listMessages(String accessToken, String labelId, int maxResults) {
        RestClient client = restClient(accessToken);

        // Step 1: list message IDs
        Map<String, Object> listResp = client.get()
                .uri("/messages?labelIds={label}&maxResults={max}", labelId, maxResults)
                .retrieve()
                .body(Map.class);

        if (listResp == null || !listResp.containsKey("messages")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> msgRefs = (List<Map<String, Object>>) listResp.get("messages");
        List<EmailSummary> summaries = new ArrayList<>();

        for (Map<String, Object> ref : msgRefs) {
            String id = (String) ref.get("id");
            String threadId = (String) ref.get("threadId");
            try {
                // Step 2: fetch metadata for each message
                Map<String, Object> msg = client.get()
                        .uri("/messages/{id}?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date",
                                id)
                        .retrieve()
                        .body(Map.class);

                if (msg == null) continue;

                List<Map<String, String>> headers = (List<Map<String, String>>)
                        ((Map<String, Object>) msg.get("payload")).get("headers");

                String from = extractHeader(headers, "From");
                String subject = extractHeader(headers, "Subject");
                String dateStr = formatEpochMs(msg.get("internalDate"));
                String snippet = (String) msg.getOrDefault("snippet", "");

                summaries.add(new EmailSummary(id, threadId, from, subject, dateStr, snippet));
            } catch (Exception e) {
                log.warn("Failed to fetch metadata for message {}: {}", id, e.getMessage());
            }
        }

        return summaries;
    }

    // -------------------------------------------------------------------------
    // Get full message
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public EmailMessage getMessage(String accessToken, String messageId) {
        RestClient client = restClient(accessToken);

        Map<String, Object> msg = client.get()
                .uri("/messages/{id}?format=full", messageId)
                .retrieve()
                .body(Map.class);

        if (msg == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }

        Map<String, Object> payload = (Map<String, Object>) msg.get("payload");
        List<Map<String, String>> headers = (List<Map<String, String>>) payload.get("headers");

        EmailMessage email = new EmailMessage();
        email.setId(messageId);
        email.setThreadId((String) msg.get("threadId"));
        email.setFrom(extractHeader(headers, "From"));
        email.setTo(extractHeader(headers, "To"));
        email.setSubject(extractHeader(headers, "Subject"));
        email.setDate(formatEpochMs(msg.get("internalDate")));
        email.setMessageId(extractHeader(headers, "Message-ID"));
        email.setReferences(extractHeader(headers, "References"));

        // Decode body (handles plain, HTML, and multipart)
        String[] bodies = decodeBody(payload);
        email.setBodyText(bodies[0]);
        email.setBodyHtml(bodies[1]);

        return email;
    }

    // -------------------------------------------------------------------------
    // List labels
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<LabelInfo> listLabels(String accessToken) {
        Map<String, Object> resp = restClient(accessToken).get()
                .uri("/labels")
                .retrieve()
                .body(Map.class);

        if (resp == null || !resp.containsKey("labels")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rawLabels = (List<Map<String, Object>>) resp.get("labels");
        List<LabelInfo> labels = new ArrayList<>();
        for (Map<String, Object> raw : rawLabels) {
            labels.add(new LabelInfo(
                    (String) raw.get("id"),
                    (String) raw.get("name"),
                    (String) raw.get("type")
            ));
        }
        // Sort: system labels first, then user labels alphabetically
        labels.sort(Comparator.comparing((LabelInfo l) -> !"system".equals(l.getType()))
                .thenComparing(LabelInfo::getName));
        return labels;
    }

    // -------------------------------------------------------------------------
    // Send / Reply
    // -------------------------------------------------------------------------
    public void sendMessage(String accessToken, String to, String subject, String body,
                            String inReplyTo, String references, String threadId) {
        String raw = buildRawMime(to, subject, body, inReplyTo, references);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("raw", raw);
        if (threadId != null && !threadId.isBlank()) {
            requestBody.put("threadId", threadId);
        }

        restClient(accessToken).post()
                .uri("/messages/send")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractHeader(List<Map<String, String>> headers, String name) {
        if (headers == null) return "";
        return headers.stream()
                .filter(h -> name.equalsIgnoreCase(h.get("name")))
                .map(h -> h.get("value"))
                .findFirst()
                .orElse("");
    }

    private String formatEpochMs(Object internalDate) {
        if (internalDate == null) return "";
        try {
            long ms = Long.parseLong(internalDate.toString());
            return DATE_FMT.format(Instant.ofEpochMilli(ms));
        } catch (NumberFormatException e) {
            return internalDate.toString();
        }
    }

    /**
     * Decodes the email body from the Gmail API payload.
     * Returns String[2]: [0] = plain text body, [1] = HTML body.
     * Handles text/plain, text/html, and multipart/* recursively.
     */
    @SuppressWarnings("unchecked")
    private String[] decodeBody(Map<String, Object> payload) {
        String[] result = {"", ""};
        if (payload == null) return result;

        String mimeType = (String) payload.getOrDefault("mimeType", "");

        if ("text/plain".equals(mimeType)) {
            result[0] = decodePartData(payload);
        } else if ("text/html".equals(mimeType)) {
            result[1] = decodePartData(payload);
        } else if (mimeType.startsWith("multipart/")) {
            List<Map<String, Object>> parts = (List<Map<String, Object>>) payload.get("parts");
            if (parts != null) {
                for (Map<String, Object> part : parts) {
                    String[] sub = decodeBody(part);
                    if (!sub[0].isEmpty()) result[0] = sub[0];
                    if (!sub[1].isEmpty()) result[1] = sub[1];
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String decodePartData(Map<String, Object> part) {
        Map<String, Object> body = (Map<String, Object>) part.get("body");
        if (body == null) return "";
        String data = (String) body.get("data");
        if (data == null || data.isBlank()) return "";
        // Gmail API uses base64url encoding — must use URL decoder
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String buildRawMime(String to, String subject, String body,
                                 String inReplyTo, String references) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage mime = new MimeMessage(session);
            mime.setFrom(new InternetAddress("me"));
            mime.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mime.setSubject(subject, "UTF-8");
            mime.setText(body, "UTF-8");

            if (inReplyTo != null && !inReplyTo.isBlank()) {
                mime.setHeader("In-Reply-To", inReplyTo);
            }
            if (references != null && !references.isBlank()) {
                mime.setHeader("References", references);
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            mime.writeTo(buf);

            // Gmail API requires base64url encoding of the raw RFC 2822 message
            return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to build MIME message", e);
        }
    }
}
