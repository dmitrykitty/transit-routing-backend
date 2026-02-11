package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.InputStream;


public interface GtfsFileProcessor {
    void process(InputStream inputStream, String cityName);
    String getName();
    int getDeletionPriority();
    void clear();


    default CsvParser createCsvParser() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        return new CsvParser(settings);
    }
}
