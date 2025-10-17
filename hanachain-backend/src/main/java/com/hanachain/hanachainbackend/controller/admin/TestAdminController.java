package com.hanachain.hanachainbackend.controller.admin;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test")
public class TestAdminController {
    
    @GetMapping
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("Test successful", "Test admin controller is working"));
    }
}