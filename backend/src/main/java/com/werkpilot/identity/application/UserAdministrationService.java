package com.werkpilot.identity.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.identity.application.port.UserAccount;
import com.werkpilot.identity.application.port.UserAccountPort;
import com.werkpilot.identity.domain.UserRole;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdministrationService {

    private final UserAccountPort userAccountPort;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPort auditEventPort;

    public UserAdministrationService(
            UserAccountPort userAccountPort,
            PasswordEncoder passwordEncoder,
            AuditEventPort auditEventPort) {
        this.userAccountPort = userAccountPort;
        this.passwordEncoder = passwordEncoder;
        this.auditEventPort = auditEventPort;
    }

    @Transactional(readOnly = true)
    public UserAdministrationPage listUsers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by("email").ascending());
        Page<UserAccount> users = userAccountPort.findAll(pageRequest);
        return new UserAdministrationPage(
                users.getContent().stream().map(this::toResponse).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages());
    }

    @Transactional(readOnly = true)
    public UserAdministrationUser getUser(UUID id) {
        return userAccountPort.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public UserAdministrationUser createUser(
            AuthenticatedPrincipal actor,
            String email,
            String displayName,
            String temporaryPassword,
            Set<String> roles) {
        String normalizedEmail = normalizeEmail(email);
        Set<UserRole> normalizedRoles = normalizeRoles(roles);
        if (userAccountPort.findByEmail(normalizedEmail).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, "Email is already assigned to another user.");
        }

        UserAccount created = userAccountPort.createUser(
                normalizedEmail,
                normalizeDisplayName(displayName),
                passwordEncoder.encode(temporaryPassword),
                normalizedRoles);
        auditEventPort.append(
                AuditEventType.USER_CREATED,
                actor.userId(),
                created.id(),
                "roles=" + roleNames(created.roles()));
        return toResponse(created);
    }

    @Transactional
    public UserAdministrationUser updateUser(
            AuthenticatedPrincipal actor,
            UUID id,
            String displayName,
            boolean active,
            Set<String> roles) {
        UserAccount existing = userAccountPort.findById(id).orElseThrow(() -> notFound(id));
        Set<UserRole> normalizedRoles = normalizeRoles(roles);
        ensureLastActiveAdminRemains(existing, active, normalizedRoles);

        UserAccount updated = userAccountPort.updateUser(id, normalizeDisplayName(displayName), active, normalizedRoles);
        appendChangeAudit(actor, existing, updated);
        return toResponse(updated);
    }

    @Transactional
    public UserAdministrationUser setStatus(AuthenticatedPrincipal actor, UUID id, boolean active) {
        UserAccount existing = userAccountPort.findById(id).orElseThrow(() -> notFound(id));
        ensureLastActiveAdminRemains(existing, active, existing.roles());

        UserAccount updated = userAccountPort.updateUser(id, existing.displayName(), active, existing.roles());
        appendChangeAudit(actor, existing, updated);
        return toResponse(updated);
    }

    @Transactional
    public void disableUser(AuthenticatedPrincipal actor, UUID id) {
        setStatus(actor, id, false);
    }

    private void ensureLastActiveAdminRemains(UserAccount existing, boolean requestedActive, Set<UserRole> requestedRoles) {
        boolean wasActiveAdmin = existing.active() && existing.roles().contains(UserRole.ADMIN);
        boolean remainsActiveAdmin = requestedActive && requestedRoles.contains(UserRole.ADMIN);
        if (wasActiveAdmin && !remainsActiveAdmin && userAccountPort.countActiveAdminsExcluding(existing.id()) == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.BUSINESS_RULE_VIOLATION, "At least one active ADMIN is required.");
        }
    }

    private void appendChangeAudit(AuthenticatedPrincipal actor, UserAccount before, UserAccount after) {
        if (!before.roles().equals(after.roles())) {
            auditEventPort.append(
                    AuditEventType.USER_ROLE_CHANGED,
                    actor.userId(),
                    after.id(),
                    "from=" + roleNames(before.roles()) + ";to=" + roleNames(after.roles()));
        }
        if (before.active() != after.active()) {
            auditEventPort.append(
                    AuditEventType.USER_STATUS_CHANGED,
                    actor.userId(),
                    after.id(),
                    "from=" + before.active() + ";to=" + after.active());
        }
    }

    private UserAdministrationUser toResponse(UserAccount user) {
        return new UserAdministrationUser(
                user.id(),
                user.email(),
                user.displayName(),
                user.active(),
                roleNames(user.roles()));
    }

    private static Set<UserRole> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "At least one role is required.");
        }
        try {
            return roles.stream()
                    .map(role -> UserRole.valueOf(role.trim().toUpperCase(java.util.Locale.ROOT)))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Role is not supported.");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeDisplayName(String displayName) {
        return displayName.trim();
    }

    private static List<String> roleNames(Set<UserRole> roles) {
        return roles.stream()
                .map(Enum::name)
                .sorted()
                .toList();
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "User was not found: " + id);
    }

    public record UserAdministrationUser(
            UUID id,
            String email,
            String displayName,
            boolean active,
            List<String> roles) {

        public UserAdministrationUser {
            roles = List.copyOf(roles);
        }
    }

    public record UserAdministrationPage(
            List<UserAdministrationUser> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public UserAdministrationPage {
            items = List.copyOf(items);
        }
    }
}
