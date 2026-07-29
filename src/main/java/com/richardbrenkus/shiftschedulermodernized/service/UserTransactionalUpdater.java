package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@AllArgsConstructor
public class UserTransactionalUpdater {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public void update(UserUpdateForm updatedUser) {
        User existingUser = userRepository.findById(updatedUser.getId()).orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + updatedUser.getId()));

        /*
         * This early check catches a form that was already stale before this
         * transaction began.
         */
        if (!Objects.equals(existingUser.getVersion(), updatedUser.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(User.class, updatedUser.getId());
        }

        existingUser.setTitle(updatedUser.getTitle());
        existingUser.setName(updatedUser.getName());
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setBirthday(updatedUser.getBirthday());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setNote(updatedUser.getNote());
        existingUser.setProfession(updatedUser.getProfession());

        existingUser.getAllowedShiftTypes().clear();

        if (updatedUser.getAllowedShiftTypes() != null) {
            existingUser.getAllowedShiftTypes().addAll(updatedUser.getAllowedShiftTypes());
        }

        /*
         * Forces Hibernate to issue SQL now, while this method is executing.
         * The actual commit still happens after this method returns.
         */
        entityManager.flush();

        activityPublisher.publishSuccess(ActivityType.USER_UPDATED, "User", String.valueOf(existingUser.getId()), "Updated user '" + existingUser.getUsername() + "'");
    }
}
