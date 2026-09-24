package com.avadhoot.workforge.config;

import com.avadhoot.workforge.config.props.CorsProperties;
import com.avadhoot.workforge.config.props.JwtProperties;
import com.avadhoot.workforge.config.props.RateLimitProperties;
import com.avadhoot.workforge.config.props.SeedProperties;
import com.avadhoot.workforge.config.props.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        CorsProperties.class,
        StorageProperties.class,
        SeedProperties.class,
        RateLimitProperties.class
})
public class AppConfig {
}
