package com.richardbrenkus.hospitalshiftscheduler.entity;

import java.time.ZonedDateTime;
import java.util.*;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ValidationConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long id;

    @Version
    private Long version;

    @NotBlank(message = "{user.name.NotBlank}")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH, message = "{user.name.size}")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "{user.username.NotBlank}")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH, message = "{user.username.size}")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "{user.email.NotBlank}")
    @Email(message = "{user.email.invalid}")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "{user.password.NotBlank}")
    @Size(min = ValidationConstants.PASSWORD_MIN_LENGTH, max = ValidationConstants.PASSWORD_MAX_LENGTH, message = "{user.password.size}")
    private String password;

    private String note;

    @Column(name = "creation_date")
    private ZonedDateTime creationDate;

    private String birthday;
    private String profession;
    private String title;

    private boolean enabled = true;

    @ElementCollection
    @CollectionTable(name = "user_allowed_shift_types", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "shift_type")
    private Set<Integer> allowedShiftTypes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shift_request_id")
    private ShiftRequest shiftRequest;

    @Transient
    public boolean hasShiftRequest() {
        return shiftRequest != null;
    }

    @Transient
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

