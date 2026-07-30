package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.LandingPageRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import com.richardbrenkus.hospitalshiftscheduler.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LandingPageService service;

    @Test
    void shouldReturnZeroesWithZeroPercent_whenRepositoryIsEmpty() {
        when(userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN)).thenReturn(List.of());

        LandingPageRecord result = service.getLandingPageRecord();

        assertThat(result.userCountWithoutAdmin()).isZero();
        assertThat(result.shiftRequestCount()).isZero();
        assertThat(result.percentage()).isEqualTo("0%");
    }

    @Test
    void shouldReturnZeroPercent_whenOnlyAdminsExist() {
        // The repository already filters out admins, so callers see an empty list.
        when(userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN)).thenReturn(List.of());

        LandingPageRecord result = service.getLandingPageRecord();

        assertThat(result.userCountWithoutAdmin()).isZero();
        assertThat(result.shiftRequestCount()).isZero();
        assertThat(result.percentage()).isEqualTo("0%");
    }

    @Test
    void shouldExcludeAdminsAndComputePercentage_whenFourOfFiveNonAdminsHaveRequest() {
        User u1 = userWithRequest(2L, "freddie");
        User u2 = userWithRequest(3L, "mick");
        User u3 = userWithRequest(4L, "david");
        User u4 = userWithRequest(5L, "kurt");
        User u5NoRequest = TestFixtures.user(6L, "jim");
        when(userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN)).thenReturn(List.of(u1, u2, u3, u4, u5NoRequest));

        LandingPageRecord result = service.getLandingPageRecord();

        assertThat(result.userCountWithoutAdmin()).isEqualTo(5.0);
        assertThat(result.shiftRequestCount()).isEqualTo(4.0);
        assertThat(result.percentage()).isEqualTo("80%");
    }

    @Test
    void shouldRoundPercentageDownwards_whenSevenOfEightNonAdminsHaveRequest() {
        List<User> users = List.of(userWithRequest(1L, "u1"), userWithRequest(2L, "u2"), userWithRequest(3L, "u3"), userWithRequest(4L, "u4"), userWithRequest(5L, "u5"), userWithRequest(6L, "u6"), userWithRequest(7L, "u7"), TestFixtures.user(8L, "u8-no-request"));
        when(userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN)).thenReturn(users);

        LandingPageRecord result = service.getLandingPageRecord();

        assertThat(result.userCountWithoutAdmin()).isEqualTo(8.0);
        assertThat(result.shiftRequestCount()).isEqualTo(7.0);
        assertThat(result.percentage()).isEqualTo("88%");
    }

    private User userWithRequest(long id, String username) {
        User user = TestFixtures.user(id, username);
        user.setShiftRequest(new ShiftRequest());
        return user;
    }
}
