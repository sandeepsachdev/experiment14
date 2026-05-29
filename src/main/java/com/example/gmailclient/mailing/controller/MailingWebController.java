package com.example.gmailclient.mailing.controller;

import com.example.gmailclient.mailing.model.Campaign;
import com.example.gmailclient.mailing.model.CampaignRecipient;
import com.example.gmailclient.mailing.model.MailingList;
import com.example.gmailclient.mailing.service.CampaignService;
import com.example.gmailclient.mailing.service.MailingListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/mailing")
public class MailingWebController {

    private final MailingListService listService;
    private final CampaignService campaignService;

    public MailingWebController(MailingListService listService, CampaignService campaignService) {
        this.listService = listService;
        this.campaignService = campaignService;
    }

    // ---- Dashboard ----

    @GetMapping
    public String dashboard(Model model) {
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        List<MailingList> lists = listService.getAllLists();
        int totalContacts = listService.countAllContacts();
        int totalEmailsSent = campaigns.stream().mapToInt(Campaign::getTotalSent).sum();
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("mailingLists", lists);
        model.addAttribute("totalContacts", totalContacts);
        model.addAttribute("totalEmailsSent", totalEmailsSent);
        return "mailing/dashboard";
    }

    // ---- Mailing Lists ----

    @GetMapping("/lists")
    public String listMailingLists(Model model) {
        model.addAttribute("mailingLists", listService.getAllLists());
        return "mailing/lists";
    }

    @GetMapping("/lists/new")
    public String newListForm() {
        return "mailing/list-form";
    }

    @PostMapping("/lists")
    public String createList(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              RedirectAttributes ra) {
        try {
            MailingList list = listService.createList(name, description);
            ra.addFlashAttribute("successMessage", "Mailing list '" + list.getName() + "' created.");
            return "redirect:/mailing/lists/" + list.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/mailing/lists";
        }
    }

    @GetMapping("/lists/{id}")
    public String viewList(@PathVariable Long id, Model model) {
        MailingList list = listService.getList(id);
        model.addAttribute("mailingList", list);
        model.addAttribute("contacts", listService.getContacts(id));
        return "mailing/list-detail";
    }

    @PostMapping("/lists/{id}/contacts")
    public String addContact(@PathVariable Long id,
                              @RequestParam String email,
                              @RequestParam(required = false) String name,
                              RedirectAttributes ra) {
        try {
            listService.addContact(id, email, name);
            ra.addFlashAttribute("successMessage", "Contact added: " + email);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/mailing/lists/" + id;
    }

    @PostMapping("/lists/{id}/contacts/{contactId}/delete")
    public String deleteContact(@PathVariable Long id,
                                 @PathVariable Long contactId,
                                 RedirectAttributes ra) {
        listService.removeContact(contactId);
        ra.addFlashAttribute("successMessage", "Contact removed.");
        return "redirect:/mailing/lists/" + id;
    }

    @PostMapping("/lists/{id}/contacts/import")
    public String importContacts(@PathVariable Long id,
                                  @RequestParam String csvData,
                                  RedirectAttributes ra) {
        int count = listService.importContacts(id, csvData);
        ra.addFlashAttribute("successMessage", count + " contacts imported.");
        return "redirect:/mailing/lists/" + id;
    }

    @PostMapping("/lists/{id}/delete")
    public String deleteList(@PathVariable Long id, RedirectAttributes ra) {
        listService.deleteList(id);
        ra.addFlashAttribute("successMessage", "Mailing list deleted.");
        return "redirect:/mailing/lists";
    }

    // ---- Campaigns ----

    @GetMapping("/campaigns")
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.getAllCampaigns());
        return "mailing/campaigns";
    }

    @GetMapping("/campaigns/new")
    public String newCampaignForm() {
        return "mailing/campaign-form";
    }

    @PostMapping("/campaigns")
    public String createCampaign(@RequestParam String name,
                                   @RequestParam String subject,
                                   @RequestParam(required = false) String bodyHtml,
                                   @RequestParam(required = false) String bodyText,
                                   RedirectAttributes ra) {
        try {
            Campaign c = campaignService.createCampaign(name, subject, bodyHtml, bodyText);
            ra.addFlashAttribute("successMessage", "Campaign '" + c.getName() + "' created.");
            return "redirect:/mailing/campaigns/" + c.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/mailing/campaigns";
        }
    }

    @GetMapping("/campaigns/{id}")
    public String viewCampaign(@PathVariable Long id, Model model) {
        Campaign campaign = campaignService.getCampaign(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("mailingLists", listService.getAllLists());
        return "mailing/campaign-detail";
    }

    @GetMapping("/campaigns/{id}/edit")
    public String editCampaignForm(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignService.getCampaign(id));
        return "mailing/campaign-form";
    }

    @PostMapping("/campaigns/{id}")
    public String updateCampaign(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String subject,
                                   @RequestParam(required = false) String bodyHtml,
                                   @RequestParam(required = false) String bodyText,
                                   RedirectAttributes ra) {
        campaignService.updateCampaign(id, name, subject, bodyHtml, bodyText);
        ra.addFlashAttribute("successMessage", "Campaign updated.");
        return "redirect:/mailing/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/send")
    public String sendCampaign(@PathVariable Long id,
                                 @RequestParam List<Long> mailingListIds,
                                 RedirectAttributes ra) {
        try {
            Campaign c = campaignService.sendCampaign(id, mailingListIds);
            ra.addFlashAttribute("successMessage",
                    "Campaign sent to " + c.getTotalSent() + " recipients.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Send failed: " + e.getMessage());
        }
        return "redirect:/mailing/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/delete")
    public String deleteCampaign(@PathVariable Long id, RedirectAttributes ra) {
        campaignService.deleteCampaign(id);
        ra.addFlashAttribute("successMessage", "Campaign deleted.");
        return "redirect:/mailing/campaigns";
    }

    @GetMapping("/campaigns/{id}/recipients")
    public String viewRecipients(@PathVariable Long id, Model model) {
        Campaign campaign = campaignService.getCampaign(id);
        List<CampaignRecipient> recipients = campaignService.getRecipients(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("recipients", recipients);
        return "mailing/campaign-recipients";
    }
}
