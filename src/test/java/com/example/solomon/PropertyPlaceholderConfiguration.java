package com.example.solomon;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

// @formatter:off
// ConfigDataApplicationContextInitializer only loads application properties into the Environment.
// It does not enable @Value placeholder resolution.
// PropertyPlaceholderConfiguration registers PropertySourcesPlaceholderConfigurer,
// allowing @Value to inject properties into test fields.
// @formatter:on
@Configuration(proxyBeanMethods = false)
public class PropertyPlaceholderConfiguration {

    @Bean
    static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}