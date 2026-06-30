package com.example.rag.controller;

import com.example.rag.entity.User;
import com.example.rag.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("detail", "User not found"));
        }
        userRepository.deleteById(id);
        
        // Note: Embeddings remain in PgVector. 
        // We removed native JDBC deletion as requested.
        
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
