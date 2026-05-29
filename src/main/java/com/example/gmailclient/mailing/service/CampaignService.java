package com.example.gmailclient.mailing.service;

import com.example.gmailclient.mailing.model.*;
import com.example.gmailclient.mailing.repository.CampaignRecipientRepository;
import com.example.gmailclient.mailing.repository.CampaignRepository;
import com.example.gmailclient.mailing.repository.ContactRepository;
import com.example.gmailclient.mailing.repository.MailingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CampaignService {

    private final CampaignRepository campaignRepo;
    private final CampaignRecipientRepository recipientRepo;
    private final MailingListRepository mailingListRepo;
    private final ContactRepository contactRepo;
    private final CampaignSendService sendService;

    public CampaignService(CampaignRepository campaignRepo,
                           CampaignRecipientRepository recipientRepo,
                           MailingListRepository mailingListRepo,
                           ContactRepository contactRepo,
                           CampaignSendService sendService) {
        this.campaignRepo = campaignRepo;
        this.recipientRepo = recipientRepo;
        this.mailingListRepo = mailingListRepo;
        this.contactRepo = contactRepo;
        this.sendService = sendService;
    }

    public Campaign createCampaign(String name, String subject, String bodyHtml, String bodyText) {
        Campaign campaign = new Campaign();
        campaign.setName(name);
        campaign.setSubject(subject);
        campaign.setBodyHtml(bodyHtml);
        campaign.setBodyText(bodyText);
        campaign.setStatus(CampaignStatus.DRAFT);
        return campaignRepo.save(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaign(Long id) {
        return campaignRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaigns() {
        return campaignRepo.findAllByOrderByCreatedAtDesc();
    }

    public Campaign updateCampaign(Long id, String name, String subject, String bodyHtml, String bodyText) {
        Campaign campaign = getCampaign(id);
        campaign.setName(name);
        campaign.setSubject(subject);
        campaign.setBodyHtml(bodyHtml);
        campaign.setBodyText(bodyText);
        return campaignRepo.save(campaign);
    }

    public void deleteCampaign(Long id) {
        campaignRepo.deleteById(id);
    }

    public Campaign sendCampaign(Long campaignId, List<Long> mailingListIds) {
        Campaign campaign = getCampaign(campaignId);
        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepo.save(campaign);

        List<Contact> allContacts = new ArrayList<>();
        for (Long listId : mailingListIds) {
            allContacts.addAll(contactRepo.findByMailingListId(listId));
        }

        sendService.sendToRecipients(campaign, allContacts);

        // Refresh counters
        long sentCount = recipientRepo.countByCampaignIdAndStatus(campaignId, RecipientStatus.SENT);
        long failedCount = recipientRepo.countByCampaignIdAndStatus(campaignId, RecipientStatus.FAILED);
        campaign.setTotalSent((int) sentCount);
        campaign.setStatus(failedCount == allContacts.size() && !allContacts.isEmpty()
                ? CampaignStatus.FAILED : CampaignStatus.SENT);
        campaign.setSentAt(LocalDateTime.now());
        return campaignRepo.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignRecipient> getRecipients(Long campaignId) {
        return recipientRepo.findByCampaignId(campaignId);
    }

    public void markBounced(String messageId, String bounceReason, LocalDateTime at) {
        recipientRepo.findByMessageId(messageId).ifPresent(recipient -> {
            if (recipient.getStatus() != RecipientStatus.BOUNCED) {
                recipient.setStatus(RecipientStatus.BOUNCED);
                recipient.setBouncedAt(at);
                recipient.setBounceReason(bounceReason);
                recipientRepo.save(recipient);

                Campaign campaign = recipient.getCampaign();
                campaign.setTotalBounced(campaign.getTotalBounced() + 1);
                campaignRepo.save(campaign);
            }
        });
    }

    public void markReplied(String messageId, String replySnippet, LocalDateTime at) {
        recipientRepo.findByMessageId(messageId).ifPresent(recipient -> {
            if (recipient.getStatus() != RecipientStatus.REPLIED) {
                recipient.setStatus(RecipientStatus.REPLIED);
                recipient.setRepliedAt(at);
                recipient.setReplySnippet(replySnippet);
                recipientRepo.save(recipient);

                Campaign campaign = recipient.getCampaign();
                campaign.setTotalReplied(campaign.getTotalReplied() + 1);
                campaignRepo.save(campaign);
            }
        });
    }
}
