package com.example.walletapi.model;

import lombok.Data;
import java.util.List;

@Data
public class RequestMetadata {
    private String response_uri;
    private List<String> response_types_supported;
    private List<String> response_modes_supported;
    private List<String> id_token_signing_alg_values_supported;
    private List<String> request_object_encryption_alg_values_supported;
    private String client_id;
    private String state;
    private String request_object;
}


package com.example.walletapi.model;

import lombok.Data;

@Data
public class AuthorizationResponse {
    private String vp_token;
    private String presentation_submission;
    private String state;
}


package com.example.walletapi.controller;

import com.example.walletapi.model.AuthorizationResponse;
import com.example.walletapi.model.RequestMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/request")
public class WalletController {

    // GET /request/{id}
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequest(@PathVariable String requestId,
                                        @RequestParam(required = false) String response_code) {

        if (response_code == null) {
            return ResponseEntity.badRequest().body("Missing response_code");
        }

        return ResponseEntity.ok("Request for ID: " + requestId + " - code: " + response_code);
    }

    // POST /request/{id}
    @PostMapping("/{requestId}")
    public ResponseEntity<?> postRequestMetadata(@PathVariable String requestId,
                                                 @RequestBody RequestMetadata metadata) {
        try {
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal error");
        }
    }

    // POST /response/{id}
    @PostMapping("/response/{requestId}")
    public ResponseEntity<?> postAuthorizationResponse(@PathVariable String requestId,
                                                       @RequestBody AuthorizationResponse authResponse) {

        if (authResponse.getVp_token() == null || authResponse.getState() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        return ResponseEntity.ok("Authorization response received for request " + requestId);
    }
}