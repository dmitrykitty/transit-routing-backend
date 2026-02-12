package com.dnikitin.transit.infrastructure.importer.fileprocessor;

import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.InputStream;


public interface GtfsFileProcessor {
    void process(InputStream inputStream, CityEntity city, String source);
    String getName();
    int getDeletionPriority();
    void clear(CityEntity city);


    default CsvParser createCsvParser() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        return new CsvParser(settings);
    }
}
