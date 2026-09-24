package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByIssueId(Long issueId);
}
