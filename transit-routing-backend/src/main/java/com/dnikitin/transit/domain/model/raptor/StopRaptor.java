package com.dnikitin.transit.domain.model.raptor;

import java.io.Serializable;
import java.util.List;

public record StopRaptor(
        int id,
        List<Integer> routeIds,
        List<TransferRaptor> transferRaptors
) implements Serializable {
}
