package com.richardbrenkus.hospitalshiftscheduler.container;

import com.richardbrenkus.hospitalshiftscheduler.service.EmailReminderService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractMySqlContainerTest {

    @MockitoBean
    protected EmailReminderService emailReminderService;

    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("shift_scheduler_test").withUsername("test").withPassword("test");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
}