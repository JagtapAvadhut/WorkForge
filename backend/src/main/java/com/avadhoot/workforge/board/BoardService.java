package com.avadhoot.workforge.board;

import com.avadhoot.workforge.board.domain.Board;
import com.avadhoot.workforge.board.domain.BoardColumn;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardColumnResponse;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardColumnView;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardResponse;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardView;
import com.avadhoot.workforge.board.dto.BoardDtos.CreateBoardRequest;
import com.avadhoot.workforge.board.dto.BoardDtos.CreateColumnRequest;
import com.avadhoot.workforge.board.dto.BoardDtos.FrontendBoard;
import com.avadhoot.workforge.board.dto.BoardDtos.FrontendColumn;
import com.avadhoot.workforge.board.dto.BoardDtos.ProjectBoardData;
import com.avadhoot.workforge.board.repository.BoardColumnRepository;
import com.avadhoot.workforge.board.repository.BoardRepository;
import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.IssueMapper;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.dto.RefDtos.StatusDto;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.issue.repository.StatusRepository;
import com.avadhoot.workforge.issue.search.IssueSpecifications;
import com.avadhoot.workforge.project.domain.Project;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.workflow.WorkflowService;
import com.avadhoot.workforge.workflow.domain.WorkflowStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final ProjectRepository projectRepository;
    private final StatusRepository statusRepository;
    private final WorkflowService workflowService;

    public BoardService(BoardRepository boardRepository, BoardColumnRepository columnRepository,
                        IssueRepository issueRepository, IssueMapper issueMapper,
                        ProjectRepository projectRepository, StatusRepository statusRepository,
                        WorkflowService workflowService) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.projectRepository = projectRepository;
        this.statusRepository = statusRepository;
        this.workflowService = workflowService;
    }

    @Transactional
    public BoardResponse create(CreateBoardRequest request) {
        Board board = new Board();
        board.setProjectId(request.projectId());
        board.setName(request.name());
        if (request.type() != null) {
            board.setType(request.type());
        }
        return BoardResponse.from(boardRepository.save(board));
    }

    /**
     * Creates a default Kanban board with one column per workflow status for a new project.
     */
    @Transactional
    public Board ensureDefaultBoard(Project project) {
        List<Board> existing = boardRepository.findByProjectId(project.getId());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        Board board = new Board();
        board.setProjectId(project.getId());
        board.setName(project.getKey() + " Board");
        board.setType("KANBAN");
        board = boardRepository.save(board);

        Long workflowId = project.getWorkflowId() != null
                ? project.getWorkflowId() : workflowService.defaultWorkflow().getId();
        List<WorkflowStatus> workflowStatuses = workflowService.statuses(workflowId);
        Map<Long, Status> statusById = statusRepository.findAll().stream()
                .collect(Collectors.toMap(Status::getId, Function.identity()));

        int position = 0;
        for (WorkflowStatus ws : workflowStatuses) {
            Status status = statusById.get(ws.getStatusId());
            if (status == null) {
                continue;
            }
            BoardColumn column = new BoardColumn();
            column.setBoardId(board.getId());
            column.setName(status.getName());
            column.setStatusId(status.getId());
            column.setPosition(position++);
            columnRepository.save(column);
        }
        return board;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> list(Long projectId) {
        return boardRepository.findByProjectId(projectId).stream().map(BoardResponse::from).toList();
    }

    @Transactional
    public BoardColumnResponse addColumn(Long boardId, CreateColumnRequest request) {
        findBoard(boardId);
        BoardColumn column = new BoardColumn();
        column.setBoardId(boardId);
        column.setName(request.name());
        column.setStatusId(request.statusId());
        column.setPosition(request.position());
        column.setWipLimit(request.wipLimit());
        return BoardColumnResponse.from(columnRepository.save(column));
    }

    @Transactional(readOnly = true)
    public BoardView view(Long boardId) {
        Board board = findBoard(boardId);
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderByPosition(boardId);
        List<BoardColumnView> columnViews = columns.stream().map(column -> {
            Specification<Issue> spec = IssueSpecifications.projectId(board.getProjectId())
                    .and((root, q, cb) -> cb.equal(root.get("status").get("id"), column.getStatusId()));
            List<IssueResponse> issues = issueRepository.findAll(spec).stream()
                    .map(issueMapper::toResponse).toList();
            return new BoardColumnView(BoardColumnResponse.from(column), issues);
        }).toList();
        return new BoardView(BoardResponse.from(board), columnViews);
    }

    @Transactional(readOnly = true)
    public ProjectBoardData projectBoard(String projectKey, String sprintId) {
        Project project = projectRepository.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectKey));
        Board board = boardRepository.findByProjectId(project.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Board for project", projectKey));

        List<BoardColumn> columns = columnRepository.findByBoardIdOrderByPosition(board.getId());
        Map<Long, Status> statusById = statusRepository.findAll().stream()
                .collect(Collectors.toMap(Status::getId, Function.identity()));

        List<FrontendColumn> frontendColumns = new ArrayList<>();
        for (BoardColumn column : columns) {
            Status status = statusById.get(column.getStatusId());
            frontendColumns.add(new FrontendColumn(
                    String.valueOf(column.getId()),
                    column.getName(),
                    status != null ? status.getCategory() : null,
                    List.of(String.valueOf(column.getStatusId())),
                    column.getWipLimit()));
        }

        Specification<Issue> spec = IssueSpecifications.projectId(project.getId());
        Long sprint = CodeMappings.parseId(sprintId);
        if (sprint != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("sprintId"), sprint));
        }
        List<IssueResponse> issues = issueRepository.findAll(spec).stream()
                .map(issueMapper::toResponse).toList();

        FrontendBoard frontendBoard = new FrontendBoard(
                String.valueOf(board.getId()),
                board.getName(),
                project.getKey(),
                frontendColumns);
        return new ProjectBoardData(frontendBoard, issues);
    }

    @Transactional(readOnly = true)
    public List<StatusDto> workflowStatuses(String projectKey) {
        Project project = projectRepository.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectKey));
        Long workflowId = project.getWorkflowId() != null
                ? project.getWorkflowId() : workflowService.defaultWorkflow().getId();
        Map<Long, Status> statusById = statusRepository.findAll().stream()
                .collect(Collectors.toMap(Status::getId, Function.identity()));
        return workflowService.statuses(workflowId).stream()
                .map(ws -> {
                    Status status = statusById.get(ws.getStatusId());
                    return StatusDto.from(status, ws.getPosition());
                })
                .filter(s -> s != null)
                .toList();
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }
}
