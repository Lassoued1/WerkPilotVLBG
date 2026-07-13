package com.werkpilot.identity.api;

import com.werkpilot.identity.application.UserAdministrationService;
import com.werkpilot.identity.application.UserAdministrationService.UserAdministrationPage;
import com.werkpilot.identity.application.UserAdministrationService.UserAdministrationUser;
import com.werkpilot.identity.application.PasswordResetService;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserAdministrationController {

    private final UserAdministrationService userAdministrationService;
    private final PasswordResetService passwordResetService;

    public UserAdministrationController(
            UserAdministrationService userAdministrationService,
            PasswordResetService passwordResetService) {
        this.userAdministrationService = userAdministrationService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping
    PageResponse<UserAdministrationUser> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        UserAdministrationPage users = userAdministrationService.listUsers(page, size);
        return new PageResponse<>(
                users.items(),
                users.page(),
                users.size(),
                users.totalElements(),
                users.totalPages());
    }

    @PostMapping
    ResponseEntity<UserAdministrationUser> createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        UserAdministrationUser created = userAdministrationService.createUser(
                principal(authentication),
                request.email(),
                request.displayName(),
                request.temporaryPassword(),
                request.roles());
        return ResponseEntity.created(URI.create("/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    UserAdministrationUser getUser(@PathVariable UUID id) {
        return userAdministrationService.getUser(id);
    }

    @PutMapping("/{id}")
    UserAdministrationUser updateUser(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userAdministrationService.updateUser(
                principal(authentication),
                id,
                request.displayName(),
                request.active(),
                request.roles());
    }

    @PatchMapping("/{id}/status")
    UserAdministrationUser setStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userAdministrationService.setStatus(principal(authentication), id, request.active());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> disableUser(Authentication authentication, @PathVariable UUID id) {
        userAdministrationService.disableUser(principal(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/password-reset")
    ResponseEntity<Void> triggerPasswordReset(Authentication authentication, @PathVariable UUID id) {
        passwordResetService.adminTriggerReset(principal(authentication), id);
        return ResponseEntity.accepted().build();
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(min = 12, max = 128) String temporaryPassword,
            @NotEmpty Set<String> roles) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 160) String displayName,
            boolean active,
            @NotEmpty Set<String> roles) {
    }

    public record UpdateUserStatusRequest(boolean active) {
    }
}
