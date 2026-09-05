package gr.aueb.cf.barberapp.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Switches on @CreatedDate / @LastModifiedDate in AbstractEntity.
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
