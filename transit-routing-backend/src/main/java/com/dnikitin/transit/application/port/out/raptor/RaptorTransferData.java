package com.dnikitin.transit.application.port.out.raptor;

public record RaptorTransferData(
        long fromStopId,
        long toStopId,
        int durationInSeconds
) {
}
