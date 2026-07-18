package com.example.solomon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// @formatter:off
// @SpringBootTest is convenient, but loading the entire application context makes tests significantly slower.
// While the execution time itself may not be a major concern, the tight coupling between tests is.
// We end up maintaining the entire test environment even when a test only requires a small subset of the context.
// That's why we prefer slice tests whenever possible.
// @formatter:on
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@Import(PropertyPlaceholderConfiguration.class)
public @interface SlicedSpringContextTest {
}