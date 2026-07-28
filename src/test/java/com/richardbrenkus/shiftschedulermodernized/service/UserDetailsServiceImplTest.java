package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    void shouldReturnUserDetailsWithMappedRole_whenUserExists() {
        User user = TestFixtures.user(1L, "freddie");
        user.setPassword("encoded");
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("freddie");

        assertThat(result.getUsername()).isEqualTo("freddie");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldMapUserRoleToRoleUserAuthority_whenRoleIsUser() {
        User user = TestFixtures.user(2L, "mick");
        user.setPassword("encoded");
        when(userRepository.findByUsername("mick")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("mick");

        assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenUserRepositoryReturnsNull() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
