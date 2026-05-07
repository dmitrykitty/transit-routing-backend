package com.dnikitin.transit.infrastructure.configuration;

import com.dnikitin.transit.domain.service.RaptorRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    public RaptorRouter raptorRouter() {
        return new RaptorRouter();
    }
}
