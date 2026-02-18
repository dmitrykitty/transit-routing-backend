package com.dnikitin.transit.domain.model.raptor;

import java.util.List;

public record Stop(
        int id,
        List<Integer> routeIds,
        List<Transfer> transfers
) {
}
