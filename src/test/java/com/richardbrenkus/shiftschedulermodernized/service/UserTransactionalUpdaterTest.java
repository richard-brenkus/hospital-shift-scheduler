
package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTransactionalUpdaterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    ActivityPublisher activityPublisher;

    @InjectMocks
    private UserTransactionalUpdater updater;

    @Test
    void shouldUpdateManagedUserAndFlush() {
        User existing = TestFixtures.user(1L, "old");
        existing.setVersion(0L);
        existing.setAllowedShiftTypes(new HashSet<>(Set.of(5, 6)));

        UserUpdateForm form = new UserUpdateForm();
        form.setId(1L);
        form.setVersion(0L);
        form.setName("New Name");
        form.setUsername("newname");
        form.setEmail("new@x.test");
        form.setNote("note");
        form.setTitle("Bc.");
        form.setBirthday("1990-01-01");
        form.setProfession("Doctor");
        form.setAllowedShiftTypes(new HashSet<>(Set.of(1, 2)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        updater.update(form);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getUsername()).isEqualTo("newname");
        assertThat(existing.getEmail()).isEqualTo("new@x.test");
        assertThat(existing.getAllowedShiftTypes()).containsExactlyInAnyOrder(1, 2);

        verify(entityManager).flush();
    }

    @Test
    void shouldThrowOptimisticLockExceptionWhenVersionDiffers() {
        User existing = TestFixtures.user(1L, "old");
        existing.setVersion(3L);

        UserUpdateForm form = new UserUpdateForm();
        form.setId(1L);
        form.setVersion(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> updater.update(form))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(entityManager, never()).flush();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserDoesNotExist() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updater.update(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
