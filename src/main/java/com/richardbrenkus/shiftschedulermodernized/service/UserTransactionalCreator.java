package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;

@Service
@AllArgsConstructor
public class UserTransactionalCreator {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createUser(UserRegisterForm form) {
        User user = new User();

        user.setCreationDate(ZonedDateTime.now(ApplicationConstants.ZONE_ID));
        user.setName(form.getName().trim());
        user.setUsername(form.getUsername().trim().toLowerCase(Locale.ROOT));
        user.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setBirthday(form.getBirthday());
        user.setNote(form.getNote());
        user.setTitle(form.getTitle());
        user.setProfession(form.getProfession());
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setAllowedShiftTypes(new HashSet<>(form.getAllowedShiftTypes()));

        userRepository.save(user);
        entityManager.flush();
    }
}
