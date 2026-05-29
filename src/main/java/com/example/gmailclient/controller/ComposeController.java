package com.example.gmailclient.controller;

import com.example.gmailclient.model.EmailMessage;
import com.example.gmailclient.model.LabelInfo;
import com.example.gmailclient.service.GmailService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ComposeController {

    private final GmailService gmailService;

    public ComposeController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/compose")
    public String composeForm(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam(required = false) String replyTo,
            Model model) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        List<LabelInfo> labels = gmailService.listLabels(accessToken);
        model.addAttribute("labels", labels);

        if (replyTo != null && !replyTo.isBlank()) {
            // Pre-fill compose form for reply
            EmailMessage original = gmailService.getMessage(accessToken, replyTo);
            model.addAttribute("to", original.getFrom());
            model.addAttribute("subject", prependRe(original.getSubject()));
            model.addAttribute("body", buildQuotedReply(original));
            model.addAttribute("replyTo", replyTo);
            model.addAttribute("threadId", original.getThreadId());
            model.addAttribute("inReplyTo", original.getMessageId());
            model.addAttribute("references", buildReferences(original));
        } else {
            model.addAttribute("to", "");
            model.addAttribute("subject", "");
            model.addAttribute("body", "");
            model.addAttribute("replyTo", "");
            model.addAttribute("threadId", "");
            model.addAttribute("inReplyTo", "");
            model.addAttribute("references", "");
        }

        return "compose";
    }

    @PostMapping("/send")
    public String sendEmail(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam(defaultValue = "") String threadId,
            @RequestParam(defaultValue = "") String inReplyTo,
            @RequestParam(defaultValue = "") String references,
            RedirectAttributes redirectAttributes) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            gmailService.sendMessage(
                    accessToken, to, subject, body,
                    inReplyTo.isBlank() ? null : inReplyTo,
                    references.isBlank() ? null : references,
                    threadId.isBlank() ? null : threadId
            );
            redirectAttributes.addFlashAttribute("successMessage", "Email sent successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send email: " + e.getMessage());
            return "redirect:/compose";
        }

        return "redirect:/inbox";
    }

    private String prependRe(String subject) {
        if (subject == null || subject.isBlank()) return "Re: ";
        return subject.startsWith("Re:") ? subject : "Re: " + subject;
    }

    private String buildQuotedReply(EmailMessage original) {
        String body = original.getBodyText() != null && !original.getBodyText().isBlank()
                ? original.getBodyText()
                : "(no plain text body)";
        return "\n\n--- On " + original.getDate() + ", " + original.getFrom() + " wrote:\n"
                + body.lines().map(line -> "> " + line).reduce("", (a, b) -> a + "\n" + b);
    }

    private String buildReferences(EmailMessage original) {
        String existing = original.getReferences();
        String msgId = original.getMessageId();
        if (existing != null && !existing.isBlank()) {
            return existing + " " + msgId;
        }
        return msgId != null ? msgId : "";
    }
}
