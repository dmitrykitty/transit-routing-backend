package com.dnikitin.transit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "service_calendar_date",
        indexes = {
                @Index(name = "idx_cal_date_service", columnList = "service_id_ext"),
                @Index(name = "idx_cal_date_lookup", columnList = "date, exception_type")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCalendarDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id_ext", nullable = false)
    private String serviceIdExternal; // mapping of service_id (np. "service_3")

    @Column(name = "date", nullable = false)
    private LocalDate date; // mapping YYYYMMDD

    @Column(name = "exception_type", nullable = false)
    private Integer exceptionType; // 1 = added, 2 = removed
}
