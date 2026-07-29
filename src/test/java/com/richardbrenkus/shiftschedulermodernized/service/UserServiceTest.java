package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ValidationError;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * NOTE: This test was regenerated after UserService switched to Optional-based
 * lookup methods (findByUsername/findById), gained a ScheduleMapper dependency,
 * and after list-returning queries were pushed into named repository methods.
 * Assertions describing the old findAll+filter-in-service semantics were
 * replaced with assertions describing the current repository-query-driven
 * behaviour.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderConfig passwordEncoderConfig;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserTransactionalUpdater userTransactionalUpdater;

    @Mock
    private UserTransactionalCreator userTransactionalCreator;

    @Mock
    private ActivityPublisher activityPublisher;

    @Mock
    private ScheduleMapper scheduleMapper;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoderConfig, userMapper, userTransactionalUpdater, userTransactionalCreator, activityPublisher, scheduleMapper);
    }

    @Test
    void shouldReturnNameOnly_whenTitleIsNull() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie Mercury");
        user.setTitle(null);
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        assertThat(service.getDisplayNameByUserName("freddie")).isEqualTo("Freddie Mercury");
    }

    @Test
    void shouldPrependTitleToName_whenTitleIsSet() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie Mercury");
        user.setTitle("MUDr.");
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        assertThat(service.getDisplayNameByUserName("freddie")).isEqualTo("MUDr. Freddie Mercury");
    }

    @Test
    void shouldThrowUsernameNotFound_whenGettingDisplayNameForUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDisplayNameByUserName("ghost")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldReturnUserFromRepository_whenGettingByUsername() {
        User user = TestFixtures.user(1L, "freddie");
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        assertThat(service.getUserByUsername("freddie")).isSameAs(user);
    }

    @Test
    void shouldEncodePasswordAndSaveUser_whenChangingPassword() {
        User user = TestFixtures.user(1L, "freddie");
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));
        when(passwordEncoderConfig.passwordEncoder()).thenReturn(passwordEncoder);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");

        service.changeUserPassword("freddie", "newpass");

        assertThat(user.getPassword()).isEqualTo("encoded-newpass");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void shouldReturnValidResult_whenCreatingUniqueUser() {
        UserRegisterForm form = new UserRegisterForm();
        form.setName("Freddie Mercury");
        form.setUsername("freddie");
        form.setEmail("freddie@example.test");
        form.setPassword("Plain1");
        form.setTitle("MUDr.");
        form.setProfession("psychiatrist");
        form.setBirthday("1967-01-24");
        form.setNote("Test");
        form.setAllowedShiftTypes(new HashSet<>(Set.of(1, 2, 3)));

        UserValidationResult result = service.validateAndCreateUser(form);
        assertTrue(result.isValid());
    }

    @Test
    void shouldDelegateExistsChecksToRepository() {
        when(userRepository.existsByUsernameIgnoreCase("mick")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("m@x.test")).thenReturn(false);
        when(userRepository.existsByNameIgnoreCase("Mick")).thenReturn(true);

        assertThat(service.existsByUsernameIgnoreCase("mick")).isTrue();
        assertThat(service.existsByEmailIgnoreCase("m@x.test")).isFalse();
        assertThat(service.existsByNameIgnoreCase("Mick")).isTrue();
    }

    @Test
    void shouldDelegateToRepository_whenGettingAllUsersWithoutAdmin() {
        User b = TestFixtures.user(2L, "bruce");
        b.setName("Bruce");
        User a = TestFixtures.user(3L, "amy");
        a.setName("Amy");
        when(userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN)).thenReturn(List.of(a, b));

        List<User> users = service.getAllUsersWithoutAdminByNameAsc();

        assertThat(users).extracting(User::getName).containsExactly("Amy", "Bruce");
    }

    @Test
    void shouldReturnUserById_whenExists() {
        User user = TestFixtures.user(9L, "jim");
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        assertThat(service.getUserById(9L)).isSameAs(user);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenUserIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(99L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("99");
    }

    @Test
    void shouldReturnUsernameFromLookupById() {
        User user = TestFixtures.user(7L, "jimi");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(service.getUsernameByUserId(7L)).isEqualTo("jimi");
    }

    @Test
    void shouldDelegateToRepository_whenGettingUsersWithShiftRequest() {
        User withRequest = TestFixtures.user(1L, "freddie");
        withRequest.setName("Freddie");
        withRequest.setShiftRequest(new ShiftRequest());
        when(userRepository.findByShiftRequestIsNotNullOrderByNameAsc()).thenReturn(List.of(withRequest));

        List<User> users = service.getAllUsersWithShiftRequestByNameAsc();

        assertThat(users).extracting(User::getName).containsExactly("Freddie");
    }

    @Test
    void shouldDelegateToRepository_whenGettingAllUsersIncludingAdmins() {
        User admin = TestFixtures.admin(1L, "root");
        admin.setName("Zoe");
        User user = TestFixtures.user(2L, "mick");
        user.setName("Alan");
        when(userRepository.findAllByOrderByNameAsc()).thenReturn(List.of(user, admin));

        List<User> users = service.getAllUsersAndAdminsByNameAsc();

        assertThat(users).extracting(User::getName).containsExactly("Alan", "Zoe");
    }

    @Test
    void shouldMapUsersToViewRecords_whenListingSummaries() {
        User u = TestFixtures.user(1L, "freddie");
        u.setName("Freddie");
        when(userRepository.findAllByOrderByNameAsc()).thenReturn(List.of(u));
        UserViewRecord record = UserViewRecord.builder().userId(1L).name("Freddie").build();
        when(userMapper.entityToUserViewRecord(u)).thenReturn(record);

        List<UserViewRecord> records = service.getAllUserSummaryViewRecordsByNameAsc();

        assertThat(records).containsExactly(record);
    }

    @Test
    void shouldDelegateUpdateToTransactionalUpdater() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(1L);
        form.setUsername("new");
        form.setVersion(0);
        form.setEmail("new@x.test");
        form.setName("New");

        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot(any(), any())).thenReturn(false);

        form.setAllowedShiftTypes(new HashSet<>(Set.of(1)));

        service.validateAndUpdateUser(form);

        verify(userTransactionalUpdater).update(form);
    }

    @Test
    void shouldReturnGlobalValidationError_whenUpdatingNonexistentUser() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(99L);

        UserValidationResult result = service.validateAndUpdateUser(form);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getGlobalErrors().getFirst().message()).isEqualTo("The selected user no longer exists.");
    }

    @Test
    void shouldReturnValidResult_whenNoFieldsConflictAndShiftTypesPresent() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(1L);
        form.setUsername("new");
        form.setEmail("new@x.test");
        form.setName("New");
        form.setAllowedShiftTypes(new HashSet<>(Set.of(1)));
        when(userRepository.existsById(any())).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("new", 1L)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@x.test", 1L)).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);

        UserValidationResult result = service.validateAndUpdateUser(form);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFieldErrors()).isEmpty();
        assertThat(result.getGlobalErrors()).isEmpty();
    }

    @Test
    void shouldReturnRejectedFields_whenUniquenessAndShiftTypeChecksFail() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(1L);
        form.setUsername("dup");
        form.setEmail("dup@x.test");
        form.setName("Dup");
        form.setAllowedShiftTypes(new HashSet<>(Set.of()));
        when(userRepository.existsById(any())).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("dup", 1L)).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("dup@x.test", 1L)).thenReturn(true);
        when(userRepository.existsByNameIgnoreCaseAndIdNot("Dup", 1L)).thenReturn(true);

        UserValidationResult result = service.validateAndUpdateUser(form);

        assertThat(result.isValid()).isFalse();

        List<String> errorFields = result.getFieldErrors().stream().map(ValidationError::field).toList();

        assertThat(errorFields).containsExactly("username", "email", "name", "allowedShiftTypes");
    }

    @Test
    void shouldRejectAllowedShiftTypes_whenNull() {
        UserUpdateForm form = new UserUpdateForm();
        form.setId(1001L);
        form.setUsername("new");
        form.setEmail("new@x.test");
        form.setName("New");
        form.setAllowedShiftTypes(null);
        when(userRepository.existsById(any())).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot(any(), any())).thenReturn(false);

        UserValidationResult result = service.validateAndUpdateUser(form);

        List<String> errorFields = result.getFieldErrors().stream().map(ValidationError::field).toList();

        assertThat(errorFields).containsExactly("allowedShiftTypes");
    }

    @Test
    void shouldDelegateFindAllUsersForSelectionToMapper() {
        User u = TestFixtures.user(1L, "u");
        u.setName("Amy");
        UserViewRecord record = UserViewRecord.builder().userId(1L).name("Amy").build();
        when(userRepository.findAllByOrderByNameAsc()).thenReturn(List.of(u));
        when(userMapper.entityToUserViewRecord(u)).thenReturn(record);

        assertThat(service.findAllUsersForSelectionByNameAsc()).containsExactly(record);
    }

    @Test
    void shouldReturnNull_whenFindUserViewByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findUserViewById(99L)).isNull();
    }

    @Test
    void shouldMapUserToViewRecord_whenFindUserViewByIdFound() {
        User user = TestFixtures.user(3L, "u");
        UserViewRecord record = UserViewRecord.builder().userId(3L).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userMapper.entityToUserViewRecord(user)).thenReturn(record);

        assertThat(service.findUserViewById(3L)).isEqualTo(record);
    }

    @Test
    void shouldDelegateGetUserUpdateFormByUserIdToMapper() {
        User user = TestFixtures.user(4L, "user4");
        UserUpdateForm form = new UserUpdateForm();
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(userMapper.entityToUserUpdateFormByUserId(user)).thenReturn(form);

        assertThat(service.getUserUpdateFormByUserId(4L)).isSameAs(form);
    }

    @Test
    void shouldDelegateDeleteUserToRepository() {
        User user = TestFixtures.user(1L, "u");

        service.deleteUser(user);

        verify(userRepository).delete(user);
    }
}
