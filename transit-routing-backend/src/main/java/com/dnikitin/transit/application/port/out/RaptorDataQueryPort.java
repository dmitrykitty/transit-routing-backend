package com.dnikitin.transit.application.port.out;

import com.dnikitin.transit.application.port.out.raptor.RaptorStopData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTransferData;
import com.dnikitin.transit.application.port.out.raptor.RaptorTripData;

import java.time.LocalDate;
import java.util.List;

public interface RaptorDataQueryPort {
    List<RaptorStopData> findStopsByCityId(Short cityId);

    List<RaptorTripData> findTripsByCityId(Short cityId, LocalDate serviceDate);

    List<RaptorTransferData> findTransfersByCityId(
            Short cityId,
            double radiusMeters,
            double walkingSpeedMetersPerSecond,
            int maxDurationInSeconds
    );
}
