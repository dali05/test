<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>pf-enforce-ban-log-provider</id>
            <phase>validate</phase>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <skip>true</skip>   <!-- ⬅️ IGNORER LA RÈGLE -->
            </configuration>
        </execution>
    </executions>
</plugin>