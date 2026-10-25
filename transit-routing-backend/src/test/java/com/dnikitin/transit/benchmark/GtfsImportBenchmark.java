package com.dnikitin.transit.benchmark;

import com.dnikitin.transit.TransitRoutingBackendApplication;
import com.dnikitin.transit.infrastructure.importer.GtfsImportService;
import com.dnikitin.transit.infrastructure.importer.GtfsProperties;
import com.dnikitin.transit.infrastructure.persistence.entity.CityEntity;
import com.dnikitin.transit.infrastructure.persistence.entity.DataImportMetadataEntity;
import com.dnikitin.transit.infrastructure.repository.CityJpaRepository;
import com.dnikitin.transit.infrastructure.repository.DataImportMetadataJpaRepository;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Times the full GTFS import of one city (GtfsImportService.performFullCityUpdate, the same code path as the
 * scheduler: clear city data, import every bundle, materialize route_stop, all in one transaction).
 * <p>
 * The configured feed archives are downloaded once into {@code <raptor.bench.dir>/gtfs} and served from a local
 * HTTP server during the timed runs, so download speed is not measured. The stored Last-Modified headers are
 * kept as they were, so the benchmark does not change what the scheduler considers up to date.
 * <p>
 * Args: cityName runs (defaults: Krakow 3). Requires PostgreSQL from docker-compose.
 */
public final class GtfsImportBenchmark {

    private static final List<String> COUNTED_TABLES = List.of(
            "agency", "service_calendar", "service_calendar_date", "route", "shape_point",
            "stop", "trip", "stop_time", "route_stop"
    );

    public static void main(String[] args) throws Exception {
        String cityName = args.length > 0 ? args[0] : "Krakow";
        int runs = Integer.parseInt(args.length > 1 ? args[1] : "3");
        Path gtfsDir = BenchmarkData.benchDir().resolve("gtfs").toAbsolutePath();
        Files.createDirectories(gtfsDir);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TransitRoutingBackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties("gtfs.initializer.enabled=false", "spring.main.banner-mode=off")
                .run()) {
            Map<String, String> remoteSources = context.getBean(GtfsProperties.class).getCities().get(cityName);
            if (remoteSources == null) {
                throw new IllegalArgumentException("No GTFS sources configured for city " + cityName);
            }

            Map<String, String> fileNames = new LinkedHashMap<>();
            for (Map.Entry<String, String> source : remoteSources.entrySet()) {
                String fileName = Path.of(URI.create(source.getValue()).getPath()).getFileName().toString();
                fileNames.put(source.getKey(), fileName);
                downloadIfMissing(source.getValue(), gtfsDir.resolve(fileName));
                System.out.printf("Feed %s: %s (%d bytes), feed_info: %s%n", source.getKey(), fileName,
                        Files.size(gtfsDir.resolve(fileName)), feedInfo(gtfsDir.resolve(fileName)));
            }

            HttpServer server = SimpleFileServer.createFileServer(
                    new InetSocketAddress("127.0.0.1", 0), gtfsDir, SimpleFileServer.OutputLevel.NONE);
            server.start();
            try {
                String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
                Map<String, String> localSources = new LinkedHashMap<>();
                fileNames.forEach((sourceId, fileName) -> localSources.put(sourceId, baseUrl + fileName));

                CityJpaRepository cityRepository = context.getBean(CityJpaRepository.class);
                CityEntity city = cityRepository.findByName(cityName)
                        .orElseGet(() -> cityRepository.save(CityEntity.builder().name(cityName).build()));
                Map<String, String> currentHeaders = currentHeaders(context, city, fileNames.keySet().stream().toList());

                GtfsImportService importService = context.getBean(GtfsImportService.class);
                for (int run = 1; run <= runs; run++) {
                    long start = System.nanoTime();
                    importService.performFullCityUpdate(city, localSources, currentHeaders);
                    System.out.printf("Import run %d: %d ms%n", run, (System.nanoTime() - start) / 1_000_000);
                }

                printRowCounts(context.getBean(JdbcTemplate.class), city);
            } finally {
                server.stop(0);
            }
        }
    }

    private static void downloadIfMissing(String url, Path target) throws Exception {
        if (Files.exists(target)) {
            return;
        }
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            HttpResponse<Path> response = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new IllegalStateException("Download of " + url + " failed with HTTP " + response.statusCode());
            }
            System.out.printf("Downloaded %s (Last-Modified: %s)%n", url,
                    response.headers().firstValue("Last-Modified").orElse("n/a"));
        }
    }

    private static String feedInfo(Path zip) throws Exception {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            ZipEntry entry = zipFile.getEntry("feed_info.txt");
            if (entry == null) {
                return "n/a";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                reader.readLine();
                String firstRow = reader.readLine();
                return firstRow == null ? "n/a" : firstRow;
            }
        }
    }

    private static Map<String, String> currentHeaders(ConfigurableApplicationContext context, CityEntity city, List<String> sourceIds) {
        DataImportMetadataJpaRepository metadataRepository = context.getBean(DataImportMetadataJpaRepository.class);
        Map<String, String> headers = new LinkedHashMap<>();
        for (String sourceId : sourceIds) {
            String header = metadataRepository.findById(sourceId)
                    .filter(metadata -> metadata.getCity().getId().equals(city.getId()))
                    .map(DataImportMetadataEntity::getLastModifiedHeader)
                    .orElse("");
            headers.put(sourceId, header == null ? "" : header);
        }
        return headers;
    }

    private static void printRowCounts(JdbcTemplate jdbcTemplate, CityEntity city) {
        long total = 0;
        StringBuilder counts = new StringBuilder();
        for (String table : COUNTED_TABLES) {
            Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE city_id = ?", Long.class, city.getId());
            total += count;
            counts.append(table).append('=').append(count).append(' ');
        }
        System.out.println("Rows after import: " + counts + "total=" + total);
    }
}
