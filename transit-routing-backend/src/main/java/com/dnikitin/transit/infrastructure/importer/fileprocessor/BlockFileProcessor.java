package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class BlockFileProcessor implements GtfsFileProcessor {
    @Getter
    private final Map<String, String> blockShiftMap = new HashMap<>();

    @Override
    public void process(InputStream inputStream, String cityName) {
        log.info("Loading block-to-shift mappings into memory...");
        CsvParser parser = createCsvParser();
        parser.iterate(inputStream).forEach(row -> {
            // row[0] = block_id, row[1] = shift
            blockShiftMap.put(row[0], row[1]);
        });

        log.info("Loaded {} block mappings", blockShiftMap.size());
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
    public void clear() {
        blockShiftMap.clear();
    }
}
