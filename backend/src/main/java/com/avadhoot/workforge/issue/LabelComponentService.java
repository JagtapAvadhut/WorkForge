package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.exception.DuplicateResourceException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Component;
import com.avadhoot.workforge.issue.domain.Label;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.ComponentResponse;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.CreateComponentRequest;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.CreateLabelRequest;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.LabelResponse;
import com.avadhoot.workforge.issue.repository.ComponentRepository;
import com.avadhoot.workforge.issue.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LabelComponentService {

    private final LabelRepository labelRepository;
    private final ComponentRepository componentRepository;

    public LabelComponentService(LabelRepository labelRepository, ComponentRepository componentRepository) {
        this.labelRepository = labelRepository;
        this.componentRepository = componentRepository;
    }

    @Transactional
    public LabelResponse createLabel(CreateLabelRequest request) {
        labelRepository.findByProjectIdAndNameIgnoreCase(request.projectId(), request.name())
                .ifPresent(l -> {
                    throw new DuplicateResourceException("Label already exists: " + request.name());
                });
        Label label = new Label();
        label.setProjectId(request.projectId());
        label.setName(request.name());
        label.setColor(request.color());
        return LabelResponse.from(labelRepository.save(label));
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> labels(Long projectId) {
        return labelRepository.findByProjectId(projectId).stream().map(LabelResponse::from).toList();
    }

    @Transactional
    public void deleteLabel(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Label", id);
        }
        labelRepository.deleteById(id);
    }

    @Transactional
    public ComponentResponse createComponent(CreateComponentRequest request) {
        if (componentRepository.existsByProjectIdAndNameIgnoreCase(request.projectId(), request.name())) {
            throw new DuplicateResourceException("Component already exists: " + request.name());
        }
        Component component = new Component();
        component.setProjectId(request.projectId());
        component.setName(request.name());
        component.setDescription(request.description());
        component.setLeadId(request.leadId());
        return ComponentResponse.from(componentRepository.save(component));
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> components(Long projectId) {
        return componentRepository.findByProjectId(projectId).stream()
                .map(ComponentResponse::from).toList();
    }

    @Transactional
    public void deleteComponent(Long id) {
        if (!componentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Component", id);
        }
        componentRepository.deleteById(id);
    }
}
