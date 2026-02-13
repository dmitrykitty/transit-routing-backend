package com.dnikitin.transit.infrastructure.repository.projection;

public interface ConnectionRow {

    String getDepStopId();

    String getArrStopId();

    Integer getDepTime();

    Integer getArrTime();

    String getTripIdExt();
}

