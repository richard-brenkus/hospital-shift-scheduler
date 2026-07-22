package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderEmailOutboxRecoveryService {

    private final ReminderEmailOutboxRepository repository;
    private final ReminderEmailOutboxRecoveryTransactionService recoveryTransactionService;
    private final Clock applicationClock;

    @Value("${planned-tasks.outbox.claim-timeout-minutes:10}")
    private long claimTimeoutMinutes;

    @Value("${planned-tasks.outbox.recovery-batch-size:100}")
    private int recoveryBatchSize;

    @Scheduled(
            fixedDelayString =
                    "${planned-tasks.outbox.recovery-delay-ms:60000}"
    )
    public void releaseStaleClaims() {
        validateConfiguration();

        LocalDateTime now = LocalDateTime.now(applicationClock);
        LocalDateTime staleBefore = now.minusMinutes(claimTimeoutMinutes);

        List<Long> staleIds = repository.findStaleProcessingIds(
                staleBefore,
                PageRequest.of(0, recoveryBatchSize)
        );

        int releasedCount = 0;

        for (Long staleId : staleIds) {
            if (staleId == null) {
                log.warn("Skipping null stale reminder outbox ID");
                continue;
            }

            try {
                boolean released = recoveryTransactionService.releaseStaleClaim(
                        staleId,
                        staleBefore,
                        now
                );

                if (released) {
                    releasedCount++;
                }

            } catch (RuntimeException exception) {
                log.error(
                        "Could not release stale reminder outbox claim {}",
                        staleId,
                        exception
                );
            }
        }

        if (releasedCount > 0) {
            log.warn(
                    "Released {} stale reminder email outbox claim(s)",
                    releasedCount
            );
        }
    }

    private void validateConfiguration() {
        if (claimTimeoutMinutes <= 0) {
            throw new IllegalStateException(
                    "planned-tasks.outbox.claim-timeout-minutes must be greater than zero"
            );
        }

        if (recoveryBatchSize <= 0) {
            throw new IllegalStateException(
                    "planned-tasks.outbox.recovery-batch-size must be greater than zero"
            );
        }
    }
}