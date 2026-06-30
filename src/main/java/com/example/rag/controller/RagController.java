package com.example.rag.controller;

import com.example.rag.service.RagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Save uploaded file temporarily
            File tempFile = File.createTempFile("uploaded-", ".pdf");
            file.transferTo(tempFile);
            Path tempFilePath = tempFile.toPath();

            // Process PDF with LangChain4j equivalent
            ragService.processPdf(tempFilePath);

            // Cleanup
            tempFile.delete();

            Map<String, String> response = new HashMap<>();
            response.put("message", "PDF processed successfully!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithPdf(@RequestBody ChatRequest request) {
        try {
            String reply = ragService.chat(request.getQuery());

            Map<String, String> response = new HashMap<>();
            response.put("reply", reply);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
