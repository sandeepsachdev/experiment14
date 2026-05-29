package com.example.gmailclient.mailing.service;

import com.example.gmailclient.mailing.model.Contact;
import com.example.gmailclient.mailing.model.MailingList;
import com.example.gmailclient.mailing.repository.ContactRepository;
import com.example.gmailclient.mailing.repository.MailingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MailingListService {

    private final MailingListRepository mailingListRepo;
    private final ContactRepository contactRepo;

    public MailingListService(MailingListRepository mailingListRepo, ContactRepository contactRepo) {
        this.mailingListRepo = mailingListRepo;
        this.contactRepo = contactRepo;
    }

    public MailingList createList(String name, String description) {
        MailingList list = new MailingList();
        list.setName(name);
        list.setDescription(description);
        return mailingListRepo.save(list);
    }

    @Transactional(readOnly = true)
    public MailingList getList(Long id) {
        return mailingListRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mailing list not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<MailingList> getAllLists() {
        return mailingListRepo.findAll();
    }

    public void deleteList(Long id) {
        mailingListRepo.deleteById(id);
    }

    public Contact addContact(Long listId, String email, String name) {
        MailingList list = getList(listId);
        if (contactRepo.existsByEmailAndMailingListId(email, listId)) {
            throw new IllegalArgumentException("Email already in list: " + email);
        }
        Contact contact = new Contact();
        contact.setEmail(email.trim().toLowerCase());
        contact.setName(name);
        contact.setMailingList(list);
        return contactRepo.save(contact);
    }

    public void removeContact(Long contactId) {
        contactRepo.deleteById(contactId);
    }

    @Transactional(readOnly = true)
    public List<Contact> getContacts(Long listId) {
        return contactRepo.findByMailingListId(listId);
    }

    @Transactional(readOnly = true)
    public int countAllContacts() {
        return (int) contactRepo.count();
    }

    public int importContacts(Long listId, String csvData) {
        if (csvData == null || csvData.isBlank()) return 0;
        MailingList list = getList(listId);
        int count = 0;
        for (String line : csvData.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", 2);
            String email = parts[0].trim().toLowerCase();
            String name = parts.length > 1 ? parts[1].trim() : "";
            if (email.isEmpty() || !email.contains("@")) continue;
            if (contactRepo.existsByEmailAndMailingListId(email, listId)) continue;
            Contact contact = new Contact();
            contact.setEmail(email);
            contact.setName(name);
            contact.setMailingList(list);
            contactRepo.save(contact);
            count++;
        }
        return count;
    }
}
