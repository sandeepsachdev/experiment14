package com.example.gmailclient.mailing.service;

import com.example.gmailclient.mailing.config.MailingProperties;
import com.example.gmailclient.mailing.model.Campaign;
import com.example.gmailclient.mailing.model.CampaignRecipient;
import com.example.gmailclient.mailing.model.Contact;
import com.example.gmailclient.mailing.model.RecipientStatus;
import com.example.gmailclient.mailing.repository.CampaignRecipientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignSendService {

    private final MailingProperties props;
    private final CampaignRecipientRepository recipientRepo;
    private final SingleEmailSender singleEmailSender;

    public CampaignSendService(MailingProperties props,
                               CampaignRecipientRepository recipientRepo,
                               SingleEmailSender singleEmailSender) {
        this.props = props;
        this.recipientRepo = recipientRepo;
        this.singleEmailSender = singleEmailSender;
    }

    public void sendToRecipients(Campaign campaign, List<Contact> contacts) {
        for (Contact contact : contacts) {
            String messageId = generateMessageId(campaign.getId());

            CampaignRecipient recipient = new CampaignRecipient();
            recipient.setCampaign(campaign);
            recipient.setEmail(contact.getEmail());
            recipient.setName(contact.getName());
            recipient.setMessageId(messageId);
            recipient.setStatus(RecipientStatus.PENDING);
            recipientRepo.save(recipient);

            // Each send runs in its own REQUIRES_NEW transaction via a separate bean
            singleEmailSender.send(recipient, campaign);
        }
    }

    private String generateMessageId(Long campaignId) {
        return "<" + UUID.randomUUID() + ".campaign" + campaignId + "@" + props.getMessageIdDomain() + ">";
    }
}
