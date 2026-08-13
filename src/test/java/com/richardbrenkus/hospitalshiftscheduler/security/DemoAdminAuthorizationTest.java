package com.richardbrenkus.hospitalshiftscheduler.security;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeName;
import com.richardbrenkus.hospitalshiftscheduler.controller.AdminController;
import com.richardbrenkus.hospitalshiftscheduler.controller.UserController;
import com.richardbrenkus.hospitalshiftscheduler.mapper.ScheduleMapper;
import com.richardbrenkus.hospitalshiftscheduler.mapper.ShiftRequestMapper;
import com.richardbrenkus.hospitalshiftscheduler.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({AdminController.class, UserController.class, DemoAdminAuthorizationTest.MethodSecurityTestConfiguration.class})
@WithMockUser(username = "demo-admin", roles = "DEMO_ADMIN")
class DemoAdminAuthorizationTest {

    private static final String ACCESS_DENIED_URL = "/403";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LandingPageService landingPageService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PlannedTasksService plannedTasksService;

    @MockitoBean
    private ShiftTypeService shiftTypeService;

    @MockitoBean
    private ShiftRequestService shiftRequestService;

    @MockitoBean
    private PrepareModelService prepareModelService;

    @MockitoBean
    private ScheduleCalculationService scheduleCalculationService;

    @MockitoBean
    private ScheduleValidationService scheduleValidationService;

    @MockitoBean
    private ScheduleMapper scheduleMapper;

    @MockitoBean
    private UserStatisticService userStatisticService;

    @MockitoBean
    private StoredScheduleService storedScheduleService;

    @MockitoBean
    private SpreadsheetService spreadsheetService;

    @MockitoBean
    private UserExcelExportService userExcelExportService;

    @MockitoBean
    private Clock applicationClock;

    @MockitoBean
    private ActivityLogCsvExportService activityLogCsvExportService;

    @MockitoBean
    private ShiftRequestMapper shiftRequestMapper;


    @Test
    @DisplayName("DEMO_ADMIN cannot save the automatic shift-request cleanup task")
    void cleanupTask_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/cleanup_task").with(csrf()).param("cleanupTaskActive", "true").param("cleanupDay", "20").param("cleanupHour", "2").param("cleanupMinute", "0")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(plannedTasksService);
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot save the email reminder task")
    void sendReminderTask_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/send_reminder_task").with(csrf()).param("sendReminderTaskActive", "true").param("startSendingRemindersDay", "20").param("startSendingRemindersHour", "8").param("startSendingRemindersMinute", "0").param("reminderSendingFrequencyInDays", "1").param("reminderRepetitions", "1").param("finalSubmissionDay", "25")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(plannedTasksService);
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot delete a user")
    void deleteUser_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/delete_user").with(csrf()).param("userId", "-1")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot delete another user's shift request through the admin endpoint")
    void deleteShiftRequestByAdmin_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/delete_shift_request").with(csrf()).param("id", "-1")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(shiftRequestService);
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot change another user's password")
    void changeUserPasswordByAdmin_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/change_user_password").with(csrf()).param("userId", "-1").param("newPassword", "ValidPassword123!").param("confirmedPassword", "ValidPassword123!")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot use the USER/ADMIN shift-request deletion endpoint")
    void deleteShiftRequestAsUser_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/user/delete_shift_request").with(csrf()).param(ModelAttributeName.USERNAME_PASSED, "some-user")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));

        verifyNoInteractions(shiftRequestService);
    }

    /*
     * ============================================================
     * Test-only security configuration
     * ============================================================
     *
     * We intentionally do NOT import the application's complete
     * WebSecurityConfig here.
     *
     * This test is concerned specifically with method security:
     *
     *     @PreAuthorize(...)
     *
     * The configuration below:
     *
     * 1. enables @PreAuthorize;
     * 2. requires authentication for HTTP requests;
     * 3. redirects denied requests to /403, matching production.
     *
     * It does NOT create a DataSource, JPA, Flyway or MySQL.
     * ============================================================
     */

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

            http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated()).exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect(request.getContextPath() + ACCESS_DENIED_URL)));

            return http.build();
        }
    }
}