package com.avadhoot.workforge.issue.dto;

import com.avadhoot.workforge.issue.domain.Component;
import com.avadhoot.workforge.issue.domain.Label;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.domain.StatusCategory;

/**
 * Small nested reference payloads shared by issue, board and picker endpoints.
 * All ids are serialised as strings to match the frontend contract.
 */
public final class RefDtos {

    private RefDtos() {
    }

    public record StatusDto(String id, String name, StatusCategory category, String color, Integer order) {
        public static StatusDto from(Status s) {
            return s == null ? null
                    : new StatusDto(String.valueOf(s.getId()), s.getName(), s.getCategory(), null, null);
        }

        public static StatusDto from(Status s, Integer order) {
            return s == null ? null
                    : new StatusDto(String.valueOf(s.getId()), s.getName(), s.getCategory(), null, order);
        }
    }

    public record LabelDto(String id, String name, String color) {
        public static LabelDto from(Label l) {
            return l == null ? null
                    : new LabelDto(String.valueOf(l.getId()), l.getName(), l.getColor());
        }
    }

    public record ComponentDto(String id, String name) {
        public static ComponentDto from(Component c) {
            return c == null ? null
                    : new ComponentDto(String.valueOf(c.getId()), c.getName());
        }
    }
}
