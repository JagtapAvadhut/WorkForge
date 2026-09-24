package com.avadhoot.workforge.attachment;

import com.avadhoot.workforge.attachment.dto.AttachmentResponse;
import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.issue.domain.Attachment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/issues/{issueId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<AttachmentResponse> upload(@PathVariable Long issueId,
                                                  @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(attachmentService.upload(issueId, file), "Attachment uploaded");
    }

    @GetMapping("/issues/{issueId}/attachments")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<AttachmentResponse>> list(@PathVariable Long issueId) {
        return ApiResponse.success(attachmentService.list(issueId));
    }

    @GetMapping("/attachments/{id}/download")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.getEntity(id);
        Resource resource = attachmentService.loadContent(attachment);
        String contentType = attachment.getContentType() != null
                ? attachment.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{id}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ApiResponse.message("Attachment deleted");
    }
}
