package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;

public record TransferRaptor(
        int destinationStopId,
        int durationInSeconds
) implements Serializable {
}
