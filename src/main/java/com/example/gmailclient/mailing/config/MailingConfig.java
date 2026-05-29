package com.example.gmailclient.mailing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Properties;

@Configuration
@EnableScheduling
public class MailingConfig {

    @Bean(name = "mailingJavaMailSender")
    public JavaMailSender mailingJavaMailSender(MailingProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getSmtp().getHost());
        sender.setPort(props.getSmtp().getPort());
        sender.setUsername(props.getSmtp().getUsername());
        sender.setPassword(props.getSmtp().getPassword());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.smtp.starttls.required", "true");
        javaMailProps.put("mail.smtp.connectiontimeout", "10000");
        javaMailProps.put("mail.smtp.timeout", "10000");
        javaMailProps.put("mail.smtp.writetimeout", "10000");

        return sender;
    }
}
