package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `notice` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1090–1097).
 * ALL 6 columns mapped 1:1.
 *
 * Key observations from actual data (lines 1103–1106):
 *  - date        : Unix epoch string (e.g. '1704841200')
 *  - description : Contains raw HTML from rich text editor
 *                  e.g. '<p>test</p>\r\n'  — render as innerHTML on frontend
 *  - type        : 'patient' (may also be 'staff', 'all', etc.)
 *  - title       : varchar(500) — the notice subject/heading
 *
 * IMPORTANT: description is raw HTML. Store and return as-is.
 * Frontend must render with innerHTML / dangerouslySetInnerHTML.
 */
@Entity
@Table(name = "notice")
@Getter
@Setter
@NoArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title", length = 500)
    private String title;

    /**
     * Notice body — raw HTML from rich text editor.
     * Must be rendered as innerHTML on the frontend.
     * varchar(100) in legacy schema — do NOT increase column size.
     */
    @Column(name = "description", length = 100)
    private String description;

    /** Unix epoch string when this notice was posted. */
    @Column(name = "date", length = 100)
    private String date;

    /** Audience type: 'patient', 'staff', 'all'. */
    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
