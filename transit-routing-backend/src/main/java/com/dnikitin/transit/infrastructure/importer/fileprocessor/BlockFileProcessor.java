package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockFileProcessor implements GtfsFileProcessor {

    //tread safe for multiple cities
    private final Map<Short, Map<String, String>> cityBlockCache = new ConcurrentHashMap<>();

    @Override
    public void process(InputStream inputStream, CityEntity city, String source) {
        log.info("Loading block-to-shift mappings into RAM for city: {}", city.getName());

        Map<String, String> blockMap = new HashMap<>();
        CsvParser parser = createCsvParser();

        parser.iterate(inputStream).forEach(row -> {
            if (row.length >= 2) {
                blockMap.put(row[0], row[1]); // block_id -> shift
            }
        });

        cityBlockCache.put(city.getId(), Collections.unmodifiableMap(blockMap));
        log.info("Successfully cached {} blocks in RAM for city ID: {}", blockMap.size(), city.getId());
    }

    public String getShiftByBlockId(String blockId, CityEntity city) {
        if (blockId == null || city.getId() == null) return null;

        Map<String, String> blockMap = cityBlockCache.get(city.getId());
        return (blockMap != null) ? blockMap.get(blockId) : null;
    }

    @Override
    public String getName() {
        return "blocks.txt";
    }

    @Override
    public int getDeletionPriority() {
        return 0;
    }

    @Override
    public void clear(CityEntity city) {
        log.info("Clearing RAM block cache for city: {}", city.getName());
        cityBlockCache.remove(city.getId());
    }
}
