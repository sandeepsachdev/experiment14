package com.example.gmailclient.mailing.service;

import com.example.gmailclient.mailing.config.MailingProperties;
import com.example.gmailclient.mailing.model.Campaign;
import com.example.gmailclient.mailing.model.CampaignRecipient;
import com.example.gmailclient.mailing.model.RecipientStatus;
import com.example.gmailclient.mailing.repository.CampaignRecipientRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Separate bean so each individual email send runs in its own REQUIRES_NEW transaction,
 * preventing a single failure from rolling back the entire campaign send.
 */
@Service
public class SingleEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SingleEmailSender.class);

    private final JavaMailSender mailSender;
    private final MailingProperties props;
    private final CampaignRecipientRepository recipientRepo;

    public SingleEmailSender(@Qualifier("mailingJavaMailSender") JavaMailSender mailSender,
                             MailingProperties props,
                             CampaignRecipientRepository recipientRepo) {
        this.mailSender = mailSender;
        this.props = props;
        this.recipientRepo = recipientRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(CampaignRecipient recipient, Campaign campaign) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(props.getSmtp().getFrom());
            helper.setTo(recipient.getEmail());
            helper.setSubject(campaign.getSubject());

            String bodyText = campaign.getBodyText() != null ? campaign.getBodyText() : "";
            String bodyHtml = campaign.getBodyHtml() != null && !campaign.getBodyHtml().isBlank()
                    ? campaign.getBodyHtml() : bodyText;
            helper.setText(bodyText, bodyHtml);

            mimeMessage.setHeader("Message-ID", recipient.getMessageId());

            mailSender.send(mimeMessage);

            recipient.setStatus(RecipientStatus.SENT);
            recipient.setSentAt(LocalDateTime.now());
            recipientRepo.save(recipient);

            log.debug("Sent campaign {} to {} with Message-ID {}",
                    campaign.getId(), recipient.getEmail(), recipient.getMessageId());
        } catch (Exception e) {
            log.error("Failed to send to {}: {}", recipient.getEmail(), e.getMessage());
            recipient.setStatus(RecipientStatus.FAILED);
            recipient.setBounceReason("Send error: " + e.getMessage());
            recipientRepo.save(recipient);
        }
    }
}
