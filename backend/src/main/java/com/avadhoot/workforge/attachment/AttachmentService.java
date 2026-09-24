package com.avadhoot.workforge.attachment;

import com.avadhoot.workforge.attachment.dto.AttachmentResponse;
import com.avadhoot.workforge.common.ErrorCode;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Attachment;
import com.avadhoot.workforge.issue.repository.AttachmentRepository;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final IssueRepository issueRepository;
    private final FileStorageService fileStorageService;

    public AttachmentService(AttachmentRepository attachmentRepository, IssueRepository issueRepository,
                             FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.issueRepository = issueRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AttachmentResponse upload(Long issueId, MultipartFile file) {
        if (issueRepository.findById(issueId).isEmpty()) {
            throw new ResourceNotFoundException("Issue", issueId);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
        }
        String storagePath;
        try {
            storagePath = fileStorageService.store(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to read uploaded file");
        }
        Attachment attachment = new Attachment();
        attachment.setIssueId(issueId);
        attachment.setFilename(file.getOriginalFilename());
        attachment.setStoragePath(storagePath);
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setUploadedBy(SecurityUtils.requireCurrentUserId());
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long issueId) {
        return attachmentRepository.findByIssueId(issueId).stream()
                .map(AttachmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Attachment getEntity(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
    }

    @Transactional(readOnly = true)
    public Resource loadContent(Attachment attachment) {
        return fileStorageService.load(attachment.getStoragePath());
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = getEntity(attachmentId);
        fileStorageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);
    }
}
