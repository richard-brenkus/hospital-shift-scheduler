package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(scripts = "/sql/users-basic.sql", executionPhase = BEFORE_TEST_METHOD)
class UserServiceIT extends AbstractMySqlContainerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoderConfig passwordEncoderConfig;

    @Test
    @Transactional
    void shouldPersistBcryptEncodedPasswordAndDefaults_whenCreatingUser() {
        UserRegisterForm form = new UserRegisterForm();
        form.setName("Charlie Doe");
        form.setUsername("charlie.doe");
        form.setEmail("charlie@example.test");
        form.setPassword("Plain1Pass");
        form.setProfession("Doctor");
        form.setBirthday("1990-01-01");
        form.setTitle("MUDr.");
        form.setNote("Integration test");
        form.setAllowedShiftTypes(Set.of(1, 2, 3));

        userService.createUser(form);

        Optional<User> saved = userRepository.findByUsername("charlie.doe");
        assertThat(saved).isPresent();
        User persisted = saved.orElseThrow();
        assertThat(persisted.getEmail()).isEqualTo("charlie@example.test");
        assertThat(persisted.getRole().name()).isEqualTo("USER");
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getCreationDate()).isNotNull();
        assertThat(persisted.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoderConfig.passwordEncoder().matches("Plain1Pass", persisted.getPassword())).isTrue();
        assertThat(persisted.getAllowedShiftTypes()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    @Transactional
    void shouldChangePasswordAndPersistBcryptHash_whenChangingPassword() {
        userService.changeUserPassword("alice.doe", "NewSecret1");

        User reloaded = userRepository.getUserByUsername("alice.doe");
        assertThat(reloaded.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoderConfig.passwordEncoder().matches("NewSecret1", reloaded.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void shouldFlushChangesFromUpdateUserThroughDirtyChecking_whenUpdatingUser() {
        User existing = userRepository.getUserByUsername("alice.doe");

        UserUpdateForm form = new UserUpdateForm();
        form.setId(existing.getId());
        form.setName("Alice Renamed");
        form.setUsername("alice.renamed");
        form.setEmail("alice.renamed@example.test");
        form.setNote("Updated");
        form.setBirthday("1990-05-05");
        form.setTitle("Bc.");
        form.setProfession("Doctor");
        form.setAllowedShiftTypes(new HashSet<>(Set.of(5, 6)));

        userService.validateAndUpdateUser(form);

        User reloaded = userRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Alice Renamed");
        assertThat(reloaded.getUsername()).isEqualTo("alice.renamed");
        assertThat(reloaded.getAllowedShiftTypes()).containsExactlyInAnyOrder(5, 6);
    }

    @Test
    void shouldReturnUsersWithoutAdminByNameAsc_whenListingWithoutAdmin() {
        assertThat(userService.getAllUsersWithoutAdminByNameAsc())
                .extracting(User::getUsername)
                .containsExactly("alice.doe", "bob.smith");
    }

    @Test
    void shouldReturnAllExistingUsernamesInLowercase_whenExistsChecksAreCalled() {
        assertThat(userService.existsByUsernameIgnoreCase("ALICE.DOE")).isTrue();
        assertThat(userService.existsByEmailIgnoreCase("BOB@example.test")).isTrue();
        assertThat(userService.existsByUsernameIgnoreCase("ghost")).isFalse();
    }
}
