package com.example.gmailclient.mailing.repository;

import com.example.gmailclient.mailing.model.CampaignRecipient;
import com.example.gmailclient.mailing.model.RecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {
    Optional<CampaignRecipient> findByMessageId(String messageId);
    List<CampaignRecipient> findByCampaignId(Long campaignId);
    long countByCampaignIdAndStatus(Long campaignId, RecipientStatus status);
}
