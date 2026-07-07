package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.export.UserExportRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.util.UserExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserExcelExportService {

    private final UserRepository userRepository;
    private final UserExcelExporter userExcelExporter;

    @Transactional(readOnly = true)
    public void exportUsers(OutputStream outputStream) throws IOException {

        List<UserExportRecord> users = new ArrayList<>();

        Iterable<User> usersIterable = userRepository.findAll();
        usersIterable.forEach(user -> users.add(toExportRecord(user)));

        userExcelExporter.export(users, outputStream);
    }

    private UserExportRecord toExportRecord(User user) {
        return new UserExportRecord(
                user.getId(),
                nullToEmpty(user.getEmail()),
                nullToEmpty(user.getName()),
                nullToEmpty(user.getUsername()),
                nullToEmpty(user.getRole().name()),
                user.isEnabled()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
