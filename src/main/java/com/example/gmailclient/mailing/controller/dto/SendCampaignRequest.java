package com.example.gmailclient.mailing.controller.dto;

import java.util.List;

public class SendCampaignRequest {
    private List<Long> mailingListIds;

    public List<Long> getMailingListIds() { return mailingListIds; }
    public void setMailingListIds(List<Long> mailingListIds) { this.mailingListIds = mailingListIds; }
}
