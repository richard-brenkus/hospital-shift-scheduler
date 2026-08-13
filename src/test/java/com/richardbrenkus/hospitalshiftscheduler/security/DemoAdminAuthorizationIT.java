package com.richardbrenkus.hospitalshiftscheduler.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("This test is disabled because it is not possible to run it in a CI environment")
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "demo-admin", roles = "DEMO_ADMIN")
class DemoAdminAuthorizationIT {

    private static final String ACCESS_DENIED_URL = "/403";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DEMO_ADMIN cannot save the automatic shift-request cleanup task")
    void cleanupTask_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/cleanup_task").with(csrf()).param("cleanupTaskActive", "true").param("cleanupDay", "20").param("cleanupHour", "2").param("cleanupMinute", "0")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot save the email reminder task")
    void sendReminderTask_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/send_reminder_task").with(csrf()).param("sendReminderTaskActive", "true").param("startSendingRemindersDay", "20").param("startSendingRemindersHour", "8").param("startSendingRemindersMinute", "0").param("reminderSendingFrequencyInDays", "1").param("reminderRepetitions", "1").param("finalSubmissionDay", "25")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot delete a user")
    void deleteUser_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/delete_user").with(csrf()).param("userId", "-1")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot delete another user's shift request through the admin endpoint")
    void deleteShiftRequestByAdmin_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/delete_shift_request").with(csrf()).param("id", "-1")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot change another user's password")
    void changeUserPasswordByAdmin_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/admin/change_user_password").with(csrf()).param("userId", "-1").param("newPassword", "ValidPassword123!").param("confirmedPassword", "ValidPassword123!")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }

    @Test
    @DisplayName("DEMO_ADMIN cannot use the USER/ADMIN shift-request deletion endpoint")
    void deleteShiftRequestAsUser_demoAdminIsDenied() throws Exception {

        mockMvc.perform(post("/user/delete_shift_request").with(csrf()).param("usernamePassed", "some-user")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(ACCESS_DENIED_URL));
    }
}