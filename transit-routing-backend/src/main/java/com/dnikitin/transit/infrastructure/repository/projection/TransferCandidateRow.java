package com.dnikitin.transit.infrastructure.repository.projection;

public interface TransferCandidateRow {
    Long getFromStopId();

    Long getToStopId();

    Integer getDurationInSeconds();
}
