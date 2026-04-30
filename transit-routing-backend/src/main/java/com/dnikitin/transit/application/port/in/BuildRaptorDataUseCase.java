package com.dnikitin.transit.application.port.in;

import com.dnikitin.transit.domain.model.raptor.RaptorDataSet;

public interface BuildRaptorDataUseCase {
    RaptorDataSet buildForCity(Short cityId);
}
