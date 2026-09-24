package com.avadhoot.workforge.attachment.dto;

import com.avadhoot.workforge.issue.domain.Attachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long issueId,
        String filename,
        String contentType,
        long sizeBytes,
        Long uploadedBy,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getIssueId(), a.getFilename(),
                a.getContentType(), a.getSizeBytes(), a.getUploadedBy(), a.getCreatedAt());
    }
}
