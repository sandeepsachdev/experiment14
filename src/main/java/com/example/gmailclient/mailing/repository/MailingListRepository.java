package com.example.gmailclient.mailing.repository;

import com.example.gmailclient.mailing.model.MailingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailingListRepository extends JpaRepository<MailingList, Long> {
    Optional<MailingList> findByName(String name);
}
