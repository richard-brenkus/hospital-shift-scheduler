package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.service.LandingPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(
        scripts = {
                //"/sql/cleanup.sql",
                "/sql/test-data.sql"
        },
        executionPhase = BEFORE_TEST_METHOD
)
class LandingPageServiceIT extends AbstractMySqlContainerTest {

    @Autowired
    private LandingPageService landingPageService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnLandingPageStatisticsFromRealMySqlData() {
        List<User> users = StreamSupport.stream(userRepository.findAll().spliterator(), false).toList();

        assertThat(users).hasSize(63);

        LandingPageRecord result = landingPageService.getLandingPageRecord();

        assertThat(result.userCountWithoutAdmin()).isEqualTo(62.0);
        assertThat(result.shiftRequestCount()).isEqualTo(59.0);
        assertThat(result.percentage()).isEqualTo("95%");
    }
}
