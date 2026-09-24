package com.avadhoot.workforge.board.dto;

import com.avadhoot.workforge.board.domain.Board;
import com.avadhoot.workforge.board.domain.BoardColumn;
import com.avadhoot.workforge.issue.domain.StatusCategory;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class BoardDtos {

    private BoardDtos() {
    }

    public record CreateBoardRequest(
            @NotNull Long projectId,
            @NotBlank @Size(max = 150) String name,
            String type) {
    }

    public record BoardResponse(Long id, Long projectId, String name, String type) {
        public static BoardResponse from(Board b) {
            return new BoardResponse(b.getId(), b.getProjectId(), b.getName(), b.getType());
        }
    }

    public record CreateColumnRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Long statusId,
            int position,
            Integer wipLimit) {
    }

    public record BoardColumnResponse(Long id, String name, Long statusId, int position, Integer wipLimit) {
        public static BoardColumnResponse from(BoardColumn c) {
            return new BoardColumnResponse(c.getId(), c.getName(), c.getStatusId(),
                    c.getPosition(), c.getWipLimit());
        }
    }

    public record BoardColumnView(BoardColumnResponse column, List<IssueResponse> issues) {
    }

    public record BoardView(BoardResponse board, List<BoardColumnView> columns) {
    }

    /** Frontend-aligned board payload used by {@code GET /projects/{key}/board}. */
    public record ProjectBoardData(FrontendBoard board, List<IssueResponse> issues) {
    }

    public record FrontendBoard(
            String id,
            String name,
            String projectKey,
            List<FrontendColumn> columns) {
    }

    public record FrontendColumn(
            String id,
            String name,
            StatusCategory category,
            List<String> statusIds,
            Integer wipLimit) {
    }
}
