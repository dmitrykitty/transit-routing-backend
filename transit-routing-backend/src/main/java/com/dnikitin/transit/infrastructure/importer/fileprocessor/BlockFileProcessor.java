package com.dnikitin.transit.infrastructure.importer.fileprocessor;

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
    private final Map<String, Map<String, String>> cityBlockCache = new ConcurrentHashMap<>();

    @Override
    public void process(InputStream inputStream, String cityName, String source) {
        log.info("Loading block-to-shift mappings into RAM for city: {}", cityName);

        Map<String, String> blockMap = new HashMap<>();

        CsvParser parser = createCsvParser();
        parser.iterate(inputStream).forEach(row -> {
            if (row.length >= 2) {
                blockMap.put(row[0], row[1]); // block_id -> shift
            }
        });

        cityBlockCache.put(cityName, Collections.unmodifiableMap(blockMap));
        log.info("Successfully cached {} blocks in RAM for {}", blockMap.size(), cityName);
    }

    public String getShiftByBlockId(String blockId, String cityName) {
        if (blockId == null || cityName == null) return null;

        Map<String, String> blockMap = cityBlockCache.get(cityName);
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
    public void clear(String cityName) {
        log.info("Clearing RAM block cache for city: {}", cityName);
        cityBlockCache.remove(cityName);
    }
}
