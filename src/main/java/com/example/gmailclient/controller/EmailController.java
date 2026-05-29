package com.example.gmailclient.controller;

import com.example.gmailclient.model.EmailMessage;
import com.example.gmailclient.model.LabelInfo;
import com.example.gmailclient.service.GmailService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class EmailController {

    private final GmailService gmailService;

    public EmailController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/email/{id}")
    public String viewEmail(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            Model model) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        EmailMessage email = gmailService.getMessage(accessToken, id);
        List<LabelInfo> labels = gmailService.listLabels(accessToken);

        model.addAttribute("email", email);
        model.addAttribute("labels", labels);

        return "email-view";
    }
}
