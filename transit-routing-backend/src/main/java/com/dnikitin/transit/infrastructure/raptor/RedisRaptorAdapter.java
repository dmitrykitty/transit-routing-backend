package com.dnikitin.transit.infrastructure.raptor;

import com.dnikitin.transit.application.port.out.RaptorRepositoryPort;
import com.dnikitin.transit.domain.model.raptor.RouteRaptor;
import com.dnikitin.transit.domain.model.raptor.StopRaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisRaptorAdapter implements RaptorRepositoryPort {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<RouteRaptor> getRoute(Short cityId, String routeId) {
        Object routeData = redisTemplate.opsForValue().get(getRouteKey(cityId, routeId));
        return Optional.ofNullable((RouteRaptor) routeData);
    }

    @Override
    public Optional<StopRaptor> getStop(Short cityId, Integer stopId) {
        Object stopData = redisTemplate.opsForValue().get(getStopKey(cityId, stopId));
        return Optional.ofNullable((StopRaptor) stopData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Long> getStopMapping(Short cityId) {
        Object stopMappingData = redisTemplate.opsForValue().get(getMappingKey(cityId));

        if (stopMappingData == null) {
            return Collections.emptyMap();
        }

        return (Map<Integer, Long>) stopMappingData;
    }

    @Override
    public void saveRoute(Short cityId, RouteRaptor routeRaptor) {
        redisTemplate.opsForValue().set(getRouteKey(cityId, String.valueOf(routeRaptor.id())), routeRaptor);
    }

    @Override
    public void saveStop(Short cityId, StopRaptor stopRaptor) {
        redisTemplate.opsForValue().set(getStopKey(cityId, stopRaptor.id()), stopRaptor);
    }

    @Override
    public void saveStopMapping(Short cityId, Map<Integer, Long> mapping) {
        redisTemplate.opsForValue().set(getMappingKey(cityId), mapping);
    }

    @Override
    public void markDataAvailable(Short cityId) {
        redisTemplate.opsForValue().set("raptor:city:" + cityId + ":status", true);
    }

    @Override
    public void clearCityData(Short cityId) {
        Set<String> keys = redisTemplate.keys("raptor:city:" + cityId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean isDataAvailable(Short cityId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("raptor:city:" + cityId + ":status"));
    }

    private String getRouteKey(Short cityId, String routeId) {
        return "raptor:city:" + cityId + ":route:" + routeId;
    }

    private String getStopKey(Short cityId, Integer stopId) {
        return "raptor:city:" + cityId + ":stop:" + stopId;
    }

    private String getMappingKey(Short cityId) {
        return "raptor:city:" + cityId + ":mapping";
    }
}
