package com.example.gmailclient.controller;

import com.example.gmailclient.model.EmailSummary;
import com.example.gmailclient.model.LabelInfo;
import com.example.gmailclient.service.GmailService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class InboxController {

    private final GmailService gmailService;

    public InboxController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/inbox")
    public String inbox(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam(defaultValue = "INBOX") String label,
            Model model) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        List<EmailSummary> messages = gmailService.listMessages(accessToken, label, 25);
        List<LabelInfo> labels = gmailService.listLabels(accessToken);

        model.addAttribute("messages", messages);
        model.addAttribute("labels", labels);
        model.addAttribute("currentLabel", label);

        return "inbox";
    }
}
