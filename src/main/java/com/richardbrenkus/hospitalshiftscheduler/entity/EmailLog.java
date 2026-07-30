package com.richardbrenkus.hospitalshiftscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "email_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Column
    @Id
    private String monthYearString;

    @Column
    private String status;

    @Column
    private String timeStamp;
}
