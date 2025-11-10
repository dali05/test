<dependencies>
    <!-- Pour utiliser HttpClient 5 avec RestTemplate -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>

    <!-- Pour le support SSL (TLS/mTLS) -->
    <dependency>
        <groupId>org.apache.httpcomponents.core5</groupId>
        <artifactId>httpcore5</artifactId>
    </dependency>

    <!-- (Déjà inclus dans Spring Boot Starter Web mais je le rappelle pour clarté) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>5.3</version>
        </dependency>
    </dependencies>
</dependencyManagement>
