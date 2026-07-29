package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.export.UserExportRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import com.richardbrenkus.shiftschedulermodernized.util.UserExcelExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserExcelExportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserExcelExporter userExcelExporter;

    @InjectMocks
    private UserExcelExportService service;

    @Test
    void shouldPassMappedUserRecordsAndStreamToExporter() throws Exception {
        User user = TestFixtures.user(7L, "freddie");
        user.setName("Freddie Mercury");
        user.setEmail("freddie@example.test");
        when(userRepository.findAll()).thenReturn(List.of(user));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        service.exportUsers(stream);

        ArgumentCaptor<List<UserExportRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(userExcelExporter).export(captor.capture(), org.mockito.ArgumentMatchers.eq(stream), org.mockito.ArgumentMatchers.any(Locale.class));

        assertThat(captor.getValue()).singleElement().satisfies(record -> {
            assertThat(record.userId()).isEqualTo(7L);
            assertThat(record.email()).isEqualTo("freddie@example.test");
            assertThat(record.name()).isEqualTo("Freddie Mercury");
            assertThat(record.username()).isEqualTo("freddie");
            assertThat(record.role()).isEqualTo("USER");
            assertThat(record.enabled()).isTrue();
        });
    }

    @Test
    void shouldConvertNullFieldsToEmptyStrings() throws Exception {
        User user = new User();
        user.setId(5L);
        user.setEnabled(false);
        user.setRole(null);
        // name, email, username, role intentionally left null
        when(userRepository.findAll()).thenReturn(List.of(user));

        service.exportUsers(new ByteArrayOutputStream());

        ArgumentCaptor<List<UserExportRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(userExcelExporter).export(captor.capture(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        UserExportRecord record = captor.getValue().getFirst();
        assertThat(record.email()).isEmpty();
        assertThat(record.name()).isEmpty();
        assertThat(record.username()).isEmpty();
        assertThat(record.role()).isEmpty();
        assertThat(record.enabled()).isFalse();
    }

    @Test
    void shouldCallExporterWithEmptyList_whenNoUsersExist() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());

        service.exportUsers(new ByteArrayOutputStream());

        ArgumentCaptor<List<UserExportRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(userExcelExporter).export(captor.capture(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void shouldMapAdminRoleToString() throws Exception {
        User admin = TestFixtures.admin(1L, "root");
        when(userRepository.findAll()).thenReturn(List.of(admin));

        service.exportUsers(new ByteArrayOutputStream());

        ArgumentCaptor<List<UserExportRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(userExcelExporter).export(captor.capture(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(captor.getValue().getFirst().role()).isEqualTo(Role.ADMIN.name());
    }
}
