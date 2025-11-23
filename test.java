@Operation(
    summary = "Playground request",
    description = "Reçoit un response_code et renvoie 200, 400 ou 500 selon la simulation.",
    responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal error")
    }
)
@PostMapping("/playground/request/{requestId}")
public ResponseEntity<Void> playgroundRequest(
        @PathVariable String requestId,
        @Parameter(example = "200") @RequestParam(required = false) String response_code) {

    if ("400".equals(response_code)) return ResponseEntity.badRequest().build();
    if ("500".equals(response_code)) return ResponseEntity.status(500).build();
    return ResponseEntity.ok().build();
}


@Operation(
    summary = "Envoi des métadonnées du Wallet pour obtenir un Request Object signé",
    requestBody = @RequestBody(
        required = true,
        content = @Content(
            mediaType = "application/x-www-form-urlencoded",
            schema = @Schema(implementation = RequestToWalle.class),
            examples = @ExampleObject(value = 
                "wallet_metadata={\n" +
                "  \"authorization_endpoint\": \"https://wallet.europe.eu/authorization\",\n" +
                "  \"response_types_supported\": [\"vp_token\"],\n" +
                "  \"response_modes_supported\": [\"form_post.jwt\"],\n" +
                "  \"vp_formats_supported\": {\n" +
                "    \"dc+sd-jwt\": {\n" +
                "      \"sd_jwt_alg_values\": [\"ES256\", \"ES384\"]\n" +
                "    }\n" +
                "  }\n" +
                "}&wallet_nonce=qPmxNfRCAT01I9rPOc8W"
            )
        )
    ),
    responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Request Object signé",
            content = @Content(
                mediaType = "application/oauth-authz-request+jwt",
                examples = @ExampleObject(
                    value = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9..."
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal error")
    }
)
@PostMapping(
        value = "/request/{requestId}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public ResponseEntity<String> createRequestObject(
        @PathVariable String requestId,
        @Parameter(hidden = true) @RequestBody MultiValueMap<String, String> formData) {
    
    try {
        String metadataJson = formData.getFirst("wallet_metadata");
        String nonce = formData.getFirst("wallet_nonce");

        if (metadataJson == null)
            return ResponseEntity.badRequest().body("wallet_metadata missing");

        // Generate JWT, etc...
        String jwt = "dummySignedJwtForExample";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/oauth-authz-request+jwt"));

        return ResponseEntity.ok().headers(headers).body(jwt);

    } catch (Exception e) {
        return ResponseEntity.status(500).body("Internal Server Error");
    }
}


@Operation(
    summary = "Réception de la réponse d'autorisation contenant le PID",
    description = "Le Wallet envoie un JWT chiffré encodé base64url dans le champ 'response'.",
    requestBody = @RequestBody(
        required = true,
        content = @Content(
            mediaType = "application/x-www-form-urlencoded",
            examples = @ExampleObject(
                value = "response=eyJhbGciOiJFUzI1NiIsImVuYyI6IkEyNTZHQ00ifQ..p0JYzIlzYW8xSjF..."
            )
        )
    ),
    responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request - invalid JWT"),
        @ApiResponse(responseCode = "500", description = "Internal error")
    }
)
@PostMapping(
        value = "/response/{requestId}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public ResponseEntity<String> handleAuthorizationResponse(
        @PathVariable String requestId,
        @Parameter(hidden = true) @RequestBody MultiValueMap<String, String> formData) {

    try {
        String encodedResponse = formData.getFirst("response");
        if (encodedResponse == null)
            return ResponseEntity.badRequest().body("Missing response");

        // Base64url decode
        byte[] decoded = Base64.getUrlDecoder().decode(encodedResponse);
        String jwt = new String(decoded);

        return ResponseEntity.ok("OK");

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("Invalid request");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Internal Server Error");
    }
}

