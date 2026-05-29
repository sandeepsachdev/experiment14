package com.example.gmailclient.mailing.repository;

import com.example.gmailclient.mailing.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByMailingListId(Long mailingListId);
    boolean existsByEmailAndMailingListId(String email, Long mailingListId);
}
