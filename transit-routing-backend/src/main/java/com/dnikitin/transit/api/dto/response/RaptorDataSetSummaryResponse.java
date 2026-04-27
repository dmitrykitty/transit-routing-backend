package com.dnikitin.transit.api.dto.response;

import java.util.List;

public record RaptorDataSetSummaryResponse(
        Short cityId,
        int stopCount,
        int routeCount,
        int transferCount,
        List<RaptorRouteSummaryResponse> routes,
        List<RaptorStopSummaryResponse> stops
) {
}
