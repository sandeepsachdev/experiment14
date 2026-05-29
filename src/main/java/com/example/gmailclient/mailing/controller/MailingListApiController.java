package com.example.gmailclient.mailing.controller;

import com.example.gmailclient.mailing.controller.dto.AddContactRequest;
import com.example.gmailclient.mailing.controller.dto.CreateListRequest;
import com.example.gmailclient.mailing.controller.dto.ImportRequest;
import com.example.gmailclient.mailing.model.Contact;
import com.example.gmailclient.mailing.model.MailingList;
import com.example.gmailclient.mailing.service.MailingListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mailing/lists")
public class MailingListApiController {

    private final MailingListService service;

    public MailingListApiController(MailingListService service) {
        this.service = service;
    }

    @GetMapping
    public List<MailingList> getAllLists() {
        return service.getAllLists();
    }

    @PostMapping
    public ResponseEntity<MailingList> createList(@RequestBody CreateListRequest req) {
        return ResponseEntity.ok(service.createList(req.getName(), req.getDescription()));
    }

    @GetMapping("/{id}")
    public MailingList getList(@PathVariable Long id) {
        return service.getList(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(@PathVariable Long id) {
        service.deleteList(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    public List<Contact> getContacts(@PathVariable Long id) {
        return service.getContacts(id);
    }

    @PostMapping("/{id}/contacts")
    public ResponseEntity<Contact> addContact(@PathVariable Long id,
                                              @RequestBody AddContactRequest req) {
        return ResponseEntity.ok(service.addContact(id, req.getEmail(), req.getName()));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<Void> removeContact(@PathVariable Long id,
                                              @PathVariable Long contactId) {
        service.removeContact(contactId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contacts/import")
    public ResponseEntity<Map<String, Integer>> importContacts(@PathVariable Long id,
                                                               @RequestBody ImportRequest req) {
        int count = service.importContacts(id, req.getCsvData());
        return ResponseEntity.ok(Map.of("imported", count));
    }
}
