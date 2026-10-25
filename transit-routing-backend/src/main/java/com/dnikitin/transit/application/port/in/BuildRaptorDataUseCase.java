package com.dnikitin.transit.application.port.in;

import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;

import java.time.LocalDate;

public interface BuildRaptorDataUseCase {
    RaptorDataSet buildForCity(Short cityId);

    RaptorDataSet buildForCity(Short cityId, LocalDate serviceDate);

    void invalidateCity(Short cityId);
}
