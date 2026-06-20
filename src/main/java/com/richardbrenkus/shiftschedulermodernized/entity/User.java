package com.richardbrenkus.shiftschedulermodernized.entity;

import java.time.ZonedDateTime;
import java.util.*;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Size(min = 2, max = 45)
    private String name;

    @NotNull
    @Size(min = 2, max = 45)
    @Column(nullable = false, unique = true)
    private String username;

    @Email
    @Column(unique = true)
    private String email;

    @NotNull
    @Size(min = 8, max = 256)
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

