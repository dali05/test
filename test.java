<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Pour décoder les JWT -->
    <dependency>
        <groupId>com.auth0</groupId>
        <artifactId>java-jwt</artifactId>
        <version>4.4.0</version>
    </dependency>
</dependencies>



@RestController
@RequestMapping("/api")
public class JwtController {

    @PostMapping("/parse")
    public Map<String, Object> parseJwt(@RequestBody Map<String, String> body) {
        String token = body.get("jwt");

        DecodedJWT jwt = JWT.decode(token);

        Map<String, Object> response = new HashMap<>();
        response.put("header", jwt.getHeader());
        response.put("payload", jwt.getClaims());
        response.put("issuer", jwt.getIssuer());
        response.put("subject", jwt.getSubject());
        response.put("expiresAt", jwt.getExpiresAt());

        return response;
    }
}