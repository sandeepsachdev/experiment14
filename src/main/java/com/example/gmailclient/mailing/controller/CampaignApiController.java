package com.example.gmailclient.mailing.controller;

import com.example.gmailclient.mailing.controller.dto.CreateCampaignRequest;
import com.example.gmailclient.mailing.controller.dto.SendCampaignRequest;
import com.example.gmailclient.mailing.model.Campaign;
import com.example.gmailclient.mailing.model.CampaignRecipient;
import com.example.gmailclient.mailing.service.CampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mailing/campaigns")
public class CampaignApiController {

    private final CampaignService service;

    public CampaignApiController(CampaignService service) {
        this.service = service;
    }

    @GetMapping
    public List<Campaign> getAllCampaigns() {
        return service.getAllCampaigns();
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(@RequestBody CreateCampaignRequest req) {
        return ResponseEntity.ok(service.createCampaign(
                req.getName(), req.getSubject(), req.getBodyHtml(), req.getBodyText()));
    }

    @GetMapping("/{id}")
    public Campaign getCampaign(@PathVariable Long id) {
        return service.getCampaign(id);
    }

    @PutMapping("/{id}")
    public Campaign updateCampaign(@PathVariable Long id, @RequestBody CreateCampaignRequest req) {
        return service.updateCampaign(id, req.getName(), req.getSubject(), req.getBodyHtml(), req.getBodyText());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        service.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Campaign> sendCampaign(@PathVariable Long id,
                                                  @RequestBody SendCampaignRequest req) {
        return ResponseEntity.ok(service.sendCampaign(id, req.getMailingListIds()));
    }

    @GetMapping("/{id}/recipients")
    public List<CampaignRecipient> getRecipients(@PathVariable Long id) {
        return service.getRecipients(id);
    }
}
