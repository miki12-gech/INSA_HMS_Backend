package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `email` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 485–493).
 * ALL 7 columns mapped 1:1.
 *
 * Represents a sent email log. Each row is a delivered email.
 * - reciepient : original legacy spelling preserved (varchar(100))
 * - user       : users.id who triggered the send
 * - date       : Unix epoch string
 * - message    : Full email body (varchar(10000))
 */
@Entity
@Table(name = "email")
@Getter
@Setter
@NoArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "subject", length = 100)
    private String subject;

    /** Unix epoch string. */
    @Column(name = "date", length = 100)
    private String date;

    /** Full email body (HTML or plain text). */
    @Column(name = "message", length = 10000)
    private String message;

    /** LEGACY SPELLING PRESERVED: 'reciepient' (not 'recipient'). */
    @Column(name = "reciepient", length = 100)
    private String reciepient;

    /** references users.id of sender. */
    @Column(name = "\"user\"", length = 100)
    private String user;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
