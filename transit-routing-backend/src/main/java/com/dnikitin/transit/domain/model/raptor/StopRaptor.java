package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;
import java.util.List;

public record StopRaptor(
        int id,
        String name,
        List<RouteAtStopRaptor> routes,
        List<TransferRaptor> transfers
) implements Serializable {
}
