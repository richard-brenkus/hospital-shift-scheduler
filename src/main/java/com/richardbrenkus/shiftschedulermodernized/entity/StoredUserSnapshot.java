package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredUserSnapshot {

    private Long userId;
    private String username;
    private String name;
    private String title;
}