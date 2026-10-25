// Closed-model load test of the journey endpoint: VUS virtual users, each sending the next
// OD query from od-pairs.csv (written by RaptorBenchmarkDataExporter) as soon as the previous one returns.
//
//   k6 run -e VUS=8 -e DURATION=60s -e QUERIES=target/bench/od-pairs.csv bench/k6/raptor-journeys.js
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CITY_ID = __ENV.CITY_ID || '1';

const queries = new SharedArray('od-pairs', () =>
    open(__ENV.QUERIES || '../../target/bench/od-pairs.csv')
        .split('\n')
        .slice(1)
        .filter((line) => line.trim().length > 0)
        .map((line) => line.trim().split(','))
);

export const options = {
    scenarios: {
        journeys: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 8),
            duration: __ENV.DURATION || '60s',
        },
    },
    summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
    const [source, target, departure, serviceDate] =
        queries[exec.scenario.iterationInTest % queries.length];
    const url = `${BASE_URL}/api/v1/raptor/cities/${CITY_ID}/journeys`
        + `?sourceStopId=${source}&targetStopId=${target}&departureTime=${departure}&serviceDate=${serviceDate}`;

    const response = http.get(url, { tags: { name: 'journeys' } });
    check(response, { 'status is 200': (r) => r.status === 200 });
}
