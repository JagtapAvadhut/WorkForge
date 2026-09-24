package com.avadhoot.workforge.board.domain;

import com.avadhoot.workforge.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "board_columns")
public class BoardColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "wip_limit")
    private Integer wipLimit;
}
