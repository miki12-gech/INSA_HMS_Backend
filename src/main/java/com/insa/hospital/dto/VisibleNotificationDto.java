package com.insa.hospital.dto;

import com.insa.hospital.entity.Notice;

/**
 * Lightweight notification payload used by the topbar and visible feed.
 *
 * Supports both persisted notice-board items and computed workflow alerts such
 * as pending medical requests or referrals.
 */
public record VisibleNotificationDto(
        Long id,
        String title,
        String description,
        String type,
        String date,
        String route
) {

    public static VisibleNotificationDto fromNotice(Notice notice) {
        return new VisibleNotificationDto(
                notice.getId() != null ? notice.getId().longValue() : null,
                notice.getTitle(),
                notice.getDescription(),
                notice.getType(),
                notice.getDate(),
                null
        );
    }
}
