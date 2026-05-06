package com.financetracker.controller;

import com.financetracker.entity.Role;
import com.financetracker.entity.User;
import com.financetracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<User> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Set<Role> roles) {
        return ResponseEntity.ok(userService.updateUserRoles(id, roles));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        userService.toggleUserEnabled(id, enabled);
        return ResponseEntity.ok().build();
    }

    // [НОВЫЙ ENDPOINT] Добавление роли пользователю
    @PostMapping("/users/{id}/roles/{role}")
    public ResponseEntity<User> addRoleToUser(
            @PathVariable Long id,
            @PathVariable Role role) {
        User user = userService.findById(id);
        user.getRoles().add(role);
        User savedUser = userService.save(user);
        return ResponseEntity.ok(savedUser);
    }

    // [НОВЫЙ ENDPOINT] Удаление роли у пользователя
    @DeleteMapping("/users/{id}/roles/{role}")
    public ResponseEntity<User> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable Role role) {
        User user = userService.findById(id);
        user.getRoles().remove(role);
        User savedUser = userService.save(user);
        return ResponseEntity.ok(savedUser);
    }
}