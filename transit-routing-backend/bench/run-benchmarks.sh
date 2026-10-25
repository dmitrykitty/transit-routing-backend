#!/usr/bin/env bash
# Runs the benchmarks of the transit routing backend against the local docker-compose PostgreSQL and
# prints a Markdown report (also saved as report.md in the run directory):
#   1. GTFS import of one city (GtfsImportBenchmark, feeds served locally, IMPORT_RUNS full imports)
#   2. RAPTOR data set export + random OD queries (RaptorBenchmarkDataExporter)
#   3. algorithm-only query latency (JMH RaptorRouterBenchmark)
#   4. cold data set build per service date (RaptorCacheBenchmark)
#   5. HTTP journey endpoint under load (k6, closed model with fixed virtual users)
#
# Usage, from transit-routing-backend/:
#   bench/run-benchmarks.sh
#
# Requirements: Docker (compose), JDK 25, curl, k6 on PATH or K6=/path/to/k6 (HTTP part is skipped without it).
# The default service date must lie inside the validity of the downloaded feed; set SERVICE_DATE otherwise.
#
# Environment overrides (defaults in brackets):
#   CITY [Krakow]  CITY_ID [1]  SERVICE_DATE [2026-10-27]  COLD_DATES ["2026-10-27 2026-10-28"]
#   QUERIES [1000]  SEED [42]  IMPORT_RUNS [3]  VUS_LIST ["1 8"]  K6_WARMUP [60s]  K6_DURATION [60s]
#   APP_HEAP [8g]  SKIP_IMPORT [0]  SKIP_JMH [0]  SKIP_HTTP [0]
#   QUICK [0]  1 = short JMH and k6 runs to check the script; such numbers are not results.
set -euo pipefail

cd "$(dirname "$0")/.."

CITY=${CITY:-Krakow}
CITY_ID=${CITY_ID:-1}
SERVICE_DATE=${SERVICE_DATE:-2026-10-27}
COLD_DATES=${COLD_DATES:-"2026-10-27 2026-10-28"}
QUERIES=${QUERIES:-1000}
SEED=${SEED:-42}
IMPORT_RUNS=${IMPORT_RUNS:-3}
VUS_LIST=${VUS_LIST:-"1 8"}
K6_WARMUP=${K6_WARMUP:-60s}
K6_DURATION=${K6_DURATION:-60s}
APP_HEAP=${APP_HEAP:-8g}
SKIP_IMPORT=${SKIP_IMPORT:-0}
SKIP_JMH=${SKIP_JMH:-0}
SKIP_HTTP=${SKIP_HTTP:-0}
QUICK=${QUICK:-0}
K6=${K6:-k6}

BENCH_DIR=target/bench
OUT=$BENCH_DIR/run-$(date +%Y%m%d-%H%M%S)
mkdir -p "$OUT"

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) WINDOWS=1; CP_SEP=';' ;;
    *) WINDOWS=0; CP_SEP=':' ;;
esac

log() { echo "[bench] $*" >&2; }

psql_query() {
    MSYS_NO_PATHCONV=1 docker compose exec -T postgres psql -U transit -d transit -At -c "$1" | tr -d '\r'
}

stop_pid() {
    if [ "$WINDOWS" = 1 ]; then taskkill //F //PID "$1" >/dev/null 2>&1 || true; else kill "$1" 2>/dev/null || true; fi
}

APP_PID=""
cleanup() { if [ -n "$APP_PID" ]; then stop_pid "$APP_PID"; fi; }
trap cleanup EXIT

hardware() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*)
            CPU=$(powershell -NoProfile -Command "(Get-CimInstance Win32_Processor).Name" | tr -d '\r')
            CORES=$(powershell -NoProfile -Command "\$p = Get-CimInstance Win32_Processor; '' + \$p.NumberOfCores + ' cores / ' + \$p.NumberOfLogicalProcessors + ' threads'" | tr -d '\r')
            RAM="$(powershell -NoProfile -Command "[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 1)" | tr -d '\r') GB"
            OS=$(powershell -NoProfile -Command "(Get-CimInstance Win32_OperatingSystem).Caption" | tr -d '\r') ;;
        Linux)
            CPU=$(lscpu | sed -n 's/^Model name: *//p')
            CORES="$(nproc) threads"
            RAM=$(awk '/MemTotal/ {printf "%.1f GB", $2 / 1048576}' /proc/meminfo)
            OS=$(. /etc/os-release && echo "$PRETTY_NAME") ;;
        Darwin)
            CPU=$(sysctl -n machdep.cpu.brand_string)
            CORES="$(sysctl -n hw.ncpu) threads"
            RAM="$(( $(sysctl -n hw.memsize) / 1073741824 )) GB"
            OS="macOS $(sw_vers -productVersion)" ;;
        *) CPU=unknown; CORES=unknown; RAM=unknown; OS=$(uname -sr) ;;
    esac
}

median() { sort -n | awk '{v[NR] = $1} END {if (NR == 0) print "n/a"; else if (NR % 2) print v[(NR + 1) / 2]; else print (v[NR / 2] + v[NR / 2 + 1]) / 2}'; }

# --- infrastructure and build --------------------------------------------------------------------------------
log "starting PostgreSQL"
docker compose up -d postgres >/dev/null
until docker compose exec -T postgres pg_isready -U transit -d transit >/dev/null 2>&1; do sleep 1; done

if [ "$(psql_query "select count(*) from information_schema.columns where table_name = 'stop_time' and column_name = 'arrival_time'")" != "0" ]; then
    log "stop_time still has time columns: apply db/manual-migrations/001_stop_time_seconds.sql first (see README)"
    exit 1
fi

log "building"
./mvnw -q -DskipTests package
./mvnw -q dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile="$OUT/classpath.txt" >/dev/null
CP="target/test-classes${CP_SEP}target/classes${CP_SEP}$(cat "$OUT/classpath.txt")"
JAR=$(ls target/transit-routing-backend-*.jar | grep -v plain | head -1)

# --- 1. GTFS import ------------------------------------------------------------------------------------------
if [ "$SKIP_IMPORT" != 1 ]; then
    log "GTFS import of $CITY, $IMPORT_RUNS run(s)"
    java -Dstdout.encoding=UTF-8 -Xms"$APP_HEAP" -Xmx"$APP_HEAP" -Draptor.bench.dir="$BENCH_DIR" -cp "$CP" \
        com.dnikitin.transit.benchmark.GtfsImportBenchmark "$CITY" "$IMPORT_RUNS" > "$OUT/import.log" 2>&1
fi

MSYS_NO_PATHCONV=1 docker compose exec -T postgres psql -U transit -d transit -q -v ON_ERROR_STOP=1 \
    < db/manual-migrations/002_stop_location_geography_index.sql

# --- 2. data set export and OD queries -----------------------------------------------------------------------
log "exporting RAPTOR data set for $SERVICE_DATE and $QUERIES OD queries"
java -Dstdout.encoding=UTF-8 -Xms"$APP_HEAP" -Xmx"$APP_HEAP" -Draptor.bench.dir="$BENCH_DIR" -cp "$CP" \
    com.dnikitin.transit.benchmark.RaptorBenchmarkDataExporter "$CITY_ID" "$SERVICE_DATE" "$QUERIES" "$SEED" > "$OUT/export.log" 2>&1

# --- 3. JMH --------------------------------------------------------------------------------------------------
if [ "$SKIP_JMH" != 1 ]; then
    log "JMH RaptorRouterBenchmark"
    JMH_OPTS=()
    if [ "$QUICK" = 1 ]; then JMH_OPTS=(-f 1 -wi 1 -i 2 -w 2s -r 3s); fi
    java -Djmh.separateClasspathJAR=true -Draptor.bench.dir="$BENCH_DIR" -cp "$CP" org.openjdk.jmh.Main RaptorRouterBenchmark \
        ${JMH_OPTS[@]+"${JMH_OPTS[@]}"} -rf json -rff "$OUT/jmh.json" > "$OUT/jmh.txt" 2>&1
fi

# --- 4. cold data set builds ---------------------------------------------------------------------------------
log "cold data set builds for $COLD_DATES"
# shellcheck disable=SC2086
java -Dstdout.encoding=UTF-8 -Xms"$APP_HEAP" -Xmx"$APP_HEAP" -cp "$CP" com.dnikitin.transit.benchmark.RaptorCacheBenchmark "$CITY_ID" $COLD_DATES > "$OUT/cache.log" 2>&1

# --- 5. HTTP with k6 -----------------------------------------------------------------------------------------
HTTP_NOTE=""
if [ "$SKIP_HTTP" = 1 ]; then
    HTTP_NOTE="skipped (SKIP_HTTP=1)"
elif ! command -v "$K6" >/dev/null 2>&1; then
    HTTP_NOTE="skipped (k6 not found; set K6=/path/to/k6)"
elif curl -s -o /dev/null http://localhost:8080/api/v1/health; then
    HTTP_NOTE="skipped (something already listens on port 8080)"
else
    log "starting the application"
    java -Xms"$APP_HEAP" -Xmx"$APP_HEAP" -jar "$JAR" --gtfs.initializer.enabled=false > "$OUT/app.log" 2>&1 &
    until grep -qE "Started TransitRoutingBackendApplication|APPLICATION FAILED" "$OUT/app.log" 2>/dev/null; do sleep 1; done
    APP_PID=$(grep -oE "with PID [0-9]+" "$OUT/app.log" | head -1 | grep -oE "[0-9]+")
    if grep -q "APPLICATION FAILED" "$OUT/app.log"; then
        log "application failed to start, see $OUT/app.log"
        exit 1
    fi

    IFS=, read -r FIRST_SOURCE FIRST_TARGET FIRST_DEPARTURE FIRST_DATE < <(sed -n 2p "$BENCH_DIR/od-pairs.csv")
    FIRST_REQUEST_S=$(curl -s -o /dev/null -w "%{time_total}" \
        "http://localhost:8080/api/v1/raptor/cities/$CITY_ID/journeys?sourceStopId=$FIRST_SOURCE&targetStopId=$FIRST_TARGET&departureTime=$FIRST_DEPARTURE&serviceDate=$FIRST_DATE")

    if [ "$WINDOWS" = 1 ]; then QUERIES_FILE="$(pwd -W)/$BENCH_DIR/od-pairs.csv"; else QUERIES_FILE="$(pwd)/$BENCH_DIR/od-pairs.csv"; fi
    if [ "$QUICK" = 1 ]; then K6_WARMUP=5s; K6_DURATION=10s; fi
    export K6_NO_USAGE_REPORT=true
    log "k6 warm-up ($K6_WARMUP, 8 VUs)"
    "$K6" run -q -e VUS=8 -e DURATION="$K6_WARMUP" -e QUERIES="$QUERIES_FILE" bench/k6/raptor-journeys.js > "$OUT/k6-warmup.txt" 2>&1
    for vus in $VUS_LIST; do
        log "k6 $vus VU(s), $K6_DURATION"
        "$K6" run -q -e VUS="$vus" -e DURATION="$K6_DURATION" -e QUERIES="$QUERIES_FILE" bench/k6/raptor-journeys.js > "$OUT/k6-vus$vus.txt" 2>&1
    done

    stop_pid "$APP_PID"
    APP_PID=""
fi

# --- report --------------------------------------------------------------------------------------------------
# Java and psql output on Windows ends lines with CRLF.
for file in "$OUT"/*.log "$OUT"/*.txt; do
    if [ -f "$file" ]; then tr -d '\r' < "$file" > "$file.tmp" && mv "$file.tmp" "$file"; fi
done
hardware
JAVA_VERSION=$(java -version 2>&1 | head -1)
GC=$(java -XX:+PrintCommandLineFlags -version 2>&1 | grep -oE "Use[A-Za-z0-9]+GC" | head -1)
COMMIT="$(git rev-parse --short HEAD)$(git diff --quiet HEAD -- src pom.xml 2>/dev/null || echo ' (uncommitted changes in src/pom.xml)')"

jmh_value() { sed -n "s/.*findJourneys:$1 *sample *\([0-9.]*\) *ms\/op.*/\1/p" "$OUT/jmh.txt"; }
k6_field() { grep -E "http_req_duration" "$2" | grep -oE "$1=[^ ]+" | head -1 | cut -d= -f2; }

{
    echo "## Benchmark results"
    echo
    echo "Run \`$(basename "$OUT")\`, commit \`$COMMIT\`$([ "$QUICK" = 1 ] && echo ', **QUICK mode: smoke test, not results**')."
    echo
    echo "| Environment | |"
    echo "|---|---|"
    echo "| CPU | $CPU ($CORES) |"
    echo "| RAM | $RAM |"
    echo "| OS | $OS |"
    echo "| JVM | $JAVA_VERSION, $GC |"
    echo "| Heap | import/export/app: -Xms$APP_HEAP -Xmx$APP_HEAP; JMH forks: -Xms4g -Xmx4g |"
    echo "| Database | PostgreSQL/PostGIS in Docker (docker-compose), same machine |"
    echo
    echo "| Network ($CITY) | |"
    echo "|---|---|"
    echo "| Stops / routes / trips / stop_times (whole feed) | $(psql_query "select count(*) from stop where city_id = $CITY_ID") / $(psql_query "select count(*) from route where city_id = $CITY_ID") / $(psql_query "select count(*) from trip where city_id = $CITY_ID") / $(psql_query "select count(*) from stop_time where city_id = $CITY_ID") |"
    echo "| RAPTOR data set for $SERVICE_DATE | $(sed -n "s/^Data set for [0-9-]*: //p" "$OUT/export.log") |"
    if [ -f "$OUT/import.log" ]; then
        grep "^Feed " "$OUT/import.log" | sed 's/^Feed \([^:]*\): \(.*\)$/| Feed \1 | \2 |/'
    fi
    echo
    if [ -f "$OUT/import.log" ]; then
        IMPORT_MS=$(sed -n 's/^Import run [0-9]*: \([0-9]*\) ms$/\1/p' "$OUT/import.log")
        echo "| GTFS import (full city, $IMPORT_RUNS run(s), feeds served locally) | |"
        echo "|---|---|"
        echo "| Runs | $(echo "$IMPORT_MS" | awk '{printf "%s%.1f s", (NR > 1 ? ", " : ""), $1 / 1000}') |"
        echo "| Median | $(echo "$IMPORT_MS" | median | awk '{printf "%.1f s", $1 / 1000}') |"
        echo "| Rows after import | $(sed -n 's/^Rows after import: //p' "$OUT/import.log") |"
        echo
    fi
    echo "| Query latency | p50 | p95 | p99 | Throughput | Samples / requests |"
    echo "|---|---|---|---|---|---|"
    if [ -f "$OUT/jmh.txt" ]; then
        SAMPLES=$(sed -n 's/.*findJourneys  *sample  *\([0-9]*\) .*/\1/p' "$OUT/jmh.txt" | head -1)
        echo "| Algorithm only (JMH, 1 thread) | $(jmh_value p0.50) ms | $(jmh_value p0.95) ms | $(jmh_value p0.99) ms | - | $SAMPLES |"
    fi
    for vus in $VUS_LIST; do
        FILE="$OUT/k6-vus$vus.txt"
        [ -f "$FILE" ] || continue
        RATE=$(grep -E "^ *http_reqs" "$FILE" | awk '{printf "%.1f req/s", $(NF)}')
        COUNT=$(grep -E "^ *http_reqs" "$FILE" | awk '{print $(NF-1)}')
        FAILED=$(grep -E "^ *http_req_failed" "$FILE" | awk '{print $2}')
        echo "| HTTP, $vus VU(s) (k6, $K6_DURATION) | $(k6_field med "$FILE") | $(k6_field 'p\(95\)' "$FILE") | $(k6_field 'p\(99\)' "$FILE") | $RATE | $COUNT (failed $FAILED) |"
    done
    [ -n "$HTTP_NOTE" ] && echo "| HTTP | $HTTP_NOTE | | | | |"
    echo
    echo "| Cold RAPTOR data set build | |"
    echo "|---|---|"
    echo "| Export run (fresh JVM, $SERVICE_DATE) | $(sed -n 's/^Data set build (cold, from PostgreSQL): //p' "$OUT/export.log") |"
    sed -n 's/^\([0-9-]*\): build \([0-9]*\) ms, heap used after GC with .* cached: \([0-9]*\) MB$/| \1 (same JVM, in order) | \2 ms, heap after GC \3 MB |/p' "$OUT/cache.log"
    sed -n 's/^\(find[A-Za-z]*\)\(([0-9-]*)\)\{0,1\}: \([0-9]*\) ms (\(.*\))$/| \1 | \3 ms (\4) |/p' "$OUT/cache.log"
    [ -n "${FIRST_REQUEST_S:-}" ] && echo "| First HTTP journey request after start | ${FIRST_REQUEST_S} s |"
    echo
    echo "Raw outputs: \`$OUT\`."
} | tee "$OUT/report.md"
