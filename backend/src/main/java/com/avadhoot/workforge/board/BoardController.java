package com.avadhoot.workforge.board;

import com.avadhoot.workforge.board.dto.BoardDtos.BoardColumnResponse;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardResponse;
import com.avadhoot.workforge.board.dto.BoardDtos.BoardView;
import com.avadhoot.workforge.board.dto.BoardDtos.CreateBoardRequest;
import com.avadhoot.workforge.board.dto.BoardDtos.CreateColumnRequest;
import com.avadhoot.workforge.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BOARD_MANAGE')")
    public ApiResponse<BoardResponse> create(@Valid @RequestBody CreateBoardRequest request) {
        return ApiResponse.success(boardService.create(request), "Board created");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<BoardResponse>> list(@RequestParam Long projectId) {
        return ApiResponse.success(boardService.list(projectId));
    }

    @PostMapping("/{boardId}/columns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BOARD_MANAGE')")
    public ApiResponse<BoardColumnResponse> addColumn(@PathVariable Long boardId,
                                                      @Valid @RequestBody CreateColumnRequest request) {
        return ApiResponse.success(boardService.addColumn(boardId, request), "Column added");
    }

    @GetMapping("/{boardId}/view")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<BoardView> view(@PathVariable Long boardId) {
        return ApiResponse.success(boardService.view(boardId));
    }
}
