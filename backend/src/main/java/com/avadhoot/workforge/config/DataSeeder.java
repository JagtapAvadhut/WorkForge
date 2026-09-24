package com.avadhoot.workforge.config;

import com.avadhoot.workforge.config.props.SeedProperties;
import com.avadhoot.workforge.issue.domain.IssueType;
import com.avadhoot.workforge.issue.domain.Priority;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.domain.StatusCategory;
import com.avadhoot.workforge.issue.repository.IssueTypeRepository;
import com.avadhoot.workforge.issue.repository.PriorityRepository;
import com.avadhoot.workforge.issue.repository.StatusRepository;
import com.avadhoot.workforge.user.domain.Permission;
import com.avadhoot.workforge.user.domain.PermissionName;
import com.avadhoot.workforge.user.domain.Role;
import com.avadhoot.workforge.user.domain.RoleName;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.domain.UserStatus;
import com.avadhoot.workforge.user.repository.PermissionRepository;
import com.avadhoot.workforge.user.repository.RoleRepository;
import com.avadhoot.workforge.user.repository.UserRepository;
import com.avadhoot.workforge.workflow.domain.Workflow;
import com.avadhoot.workforge.workflow.domain.WorkflowStatus;
import com.avadhoot.workforge.workflow.domain.WorkflowTransition;
import com.avadhoot.workforge.workflow.repository.WorkflowRepository;
import com.avadhoot.workforge.workflow.repository.WorkflowStatusRepository;
import com.avadhoot.workforge.workflow.repository.WorkflowTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Idempotently seeds RBAC reference data, issue metadata, the default workflow and
 * the bootstrap admin user. Safe to run alongside Flyway seed migrations because
 * every insert is guarded by an existence check.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    public DataSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository,
                      UserRepository userRepository, IssueTypeRepository issueTypeRepository,
                      PriorityRepository priorityRepository, StatusRepository statusRepository,
                      WorkflowRepository workflowRepository, WorkflowStatusRepository workflowStatusRepository,
                      WorkflowTransitionRepository transitionRepository, PasswordEncoder passwordEncoder,
                      SeedProperties seedProperties) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStatusRepository = workflowStatusRepository;
        this.transitionRepository = transitionRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedIssueTypes();
        seedPriorities();
        Status todo = seedStatuses();
        seedDefaultWorkflow(todo);
        seedAdminUser();
    }

    private void seedPermissions() {
        for (PermissionName name : PermissionName.values()) {
            if (permissionRepository.findByName(name).isEmpty()) {
                Permission permission = new Permission();
                permission.setName(name);
                permission.setDescription(name.name());
                permissionRepository.save(permission);
            }
        }
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            Role role = roleRepository.findByName(roleName).orElseGet(() -> {
                Role r = new Role();
                r.setName(roleName);
                r.setDescription(roleName.name());
                return r;
            });
            Set<Permission> permissions = new HashSet<>();
            for (PermissionName pn : PermissionName.forRole(roleName)) {
                permissionRepository.findByName(pn).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
            roleRepository.save(role);
        }
    }

    private void seedIssueTypes() {
        createIssueType("Task", "A task", "task", false);
        createIssueType("Bug", "A defect", "bug", false);
        createIssueType("Story", "A user story", "story", false);
        createIssueType("Epic", "A large body of work", "epic", false);
        createIssueType("Sub-task", "A subtask", "subtask", true);
    }

    private void createIssueType(String name, String description, String icon, boolean subtask) {
        if (issueTypeRepository.findByNameIgnoreCase(name).isEmpty()) {
            IssueType type = new IssueType();
            type.setName(name);
            type.setDescription(description);
            type.setIcon(icon);
            type.setSubtask(subtask);
            issueTypeRepository.save(type);
        }
    }

    private void seedPriorities() {
        createPriority("Highest", 1, "#CD1317");
        createPriority("High", 2, "#E9494A");
        createPriority("Medium", 3, "#E97F33");
        createPriority("Low", 4, "#2A8735");
        createPriority("Lowest", 5, "#57A55A");
    }

    private void createPriority(String name, int level, String color) {
        if (priorityRepository.findByNameIgnoreCase(name).isEmpty()) {
            Priority priority = new Priority();
            priority.setName(name);
            priority.setLevel(level);
            priority.setColor(color);
            priorityRepository.save(priority);
        }
    }

    private Status seedStatuses() {
        Status todo = createStatus("To Do", StatusCategory.TODO);
        createStatus("In Progress", StatusCategory.IN_PROGRESS);
        createStatus("In Review", StatusCategory.IN_PROGRESS);
        createStatus("Done", StatusCategory.DONE);
        return todo;
    }

    private Status createStatus(String name, StatusCategory category) {
        return statusRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Status status = new Status();
            status.setName(name);
            status.setCategory(category);
            return statusRepository.save(status);
        });
    }

    private void seedDefaultWorkflow(Status todo) {
        if (workflowRepository.findByIsDefaultTrue().isPresent()) {
            return;
        }
        Workflow workflow = new Workflow();
        workflow.setName("Default Workflow");
        workflow.setDescription("Standard software development workflow");
        workflow.setDefault(true);
        workflow = workflowRepository.save(workflow);

        Status inProgress = statusRepository.findByNameIgnoreCase("In Progress").orElseThrow();
        Status inReview = statusRepository.findByNameIgnoreCase("In Review").orElseThrow();
        Status done = statusRepository.findByNameIgnoreCase("Done").orElseThrow();

        addWorkflowStatus(workflow.getId(), todo.getId(), true, 0);
        addWorkflowStatus(workflow.getId(), inProgress.getId(), false, 1);
        addWorkflowStatus(workflow.getId(), inReview.getId(), false, 2);
        addWorkflowStatus(workflow.getId(), done.getId(), false, 3);

        // Forward flow
        addTransition(workflow.getId(), todo.getId(), inProgress.getId(), "Start Progress");
        addTransition(workflow.getId(), inProgress.getId(), inReview.getId(), "Submit for Review");
        addTransition(workflow.getId(), inReview.getId(), done.getId(), "Approve");
        // Backward / reopen flow
        addTransition(workflow.getId(), inProgress.getId(), todo.getId(), "Stop Progress");
        addTransition(workflow.getId(), inReview.getId(), inProgress.getId(), "Reject");
        addTransition(workflow.getId(), done.getId(), inProgress.getId(), "Reopen");
    }

    private void addWorkflowStatus(Long workflowId, Long statusId, boolean initial, int position) {
        WorkflowStatus ws = new WorkflowStatus();
        ws.setWorkflowId(workflowId);
        ws.setStatusId(statusId);
        ws.setInitial(initial);
        ws.setPosition(position);
        workflowStatusRepository.save(ws);
    }

    private void addTransition(Long workflowId, Long fromStatusId, Long toStatusId, String name) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setWorkflowId(workflowId);
        transition.setFromStatusId(fromStatusId);
        transition.setToStatusId(toStatusId);
        transition.setName(name);
        transitionRepository.save(transition);
    }

    private void seedAdminUser() {
        String username = seedProperties.getAdminUsername();
        if (userRepository.existsByUsername(username)) {
            return;
        }
        String password = seedProperties.getAdminPassword();
        if (!StringUtils.hasText(password)) {
            log.warn("Admin user '{}' not created: WORKFORGE_ADMIN_PASSWORD is not set", username);
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.SYSTEM_ADMIN).orElseThrow();
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(seedProperties.getAdminEmail());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("System Administrator");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);
        userRepository.save(admin);
        log.info("Seeded bootstrap admin user '{}'", username);
    }
}
