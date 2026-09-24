package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.audit.event.IssueEvents.CommentCreatedEvent;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Comment;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.dto.CommentResponse;
import com.avadhoot.workforge.issue.repository.CommentRepository;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.security.SecurityUtils;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(CommentRepository commentRepository, IssueRepository issueRepository,
                          UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponse add(String issueKey, String body) {
        Issue issue = findIssue(issueKey);
        Comment comment = new Comment();
        comment.setIssueId(issue.getId());
        comment.setAuthorId(SecurityUtils.requireCurrentUserId());
        comment.setBody(body);
        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentCreatedEvent(
                issue.getId(), issue.getIssueKey(), comment.getId(), comment.getAuthorId()));
        return toResponse(comment, issue.getIssueKey());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String issueKey) {
        Issue issue = findIssue(issueKey);
        return commentRepository.findByIssueId(issue.getId(),
                        PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "createdAt")))
                .stream()
                .map(c -> toResponse(c, issue.getIssueKey()))
                .toList();
    }

    @Transactional
    public CommentResponse update(String issueKey, Long commentId, String body) {
        Issue issue = findIssue(issueKey);
        Comment comment = findOwned(commentId);
        comment.setBody(body);
        return toResponse(comment, issue.getIssueKey());
    }

    @Transactional
    public void delete(String issueKey, Long commentId) {
        findIssue(issueKey);
        Comment comment = findOwned(commentId);
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment, String issueKey) {
        User author = userRepository.findById(comment.getAuthorId()).orElse(null);
        return CommentResponse.from(comment, issueKey, author);
    }

    private Issue findIssue(String issueKey) {
        return issueRepository.findByIssueKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueKey));
    }

    private Comment findOwned(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        Long current = SecurityUtils.requireCurrentUserId();
        boolean canManage = SecurityUtils.currentPrincipal()
                .map(p -> p.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("COMMENT_MANAGE")))
                .orElse(false);
        if (!comment.getAuthorId().equals(current) && !canManage) {
            throw new BusinessException("You can only modify your own comments");
        }
        return comment;
    }
}
