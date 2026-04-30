package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.*;

import java.time.LocalTime;
import java.util.*;

public class RaptorRouter {

    private static final int UNREACHED = Integer.MAX_VALUE;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    public List<RaptorJourney> findJourneys(
            RaptorDataSet dataSet,
            int sourceStopId,
            int targetStopId,
            LocalTime departureTime
    ) {
        validateStops(dataSet, sourceStopId, targetStopId);

        if (sourceStopId == targetStopId) {
            return List.of(new RaptorJourney(
                    sourceStopId,
                    targetStopId,
                    departureTime,
                    departureTime,
                    0,
                    0,
                    List.of()
            ));
        }

        int departureTimeInSeconds = departureTime.toSecondOfDay();

        Map<Integer, Integer> bestArrivalByStop = new HashMap<>();
        bestArrivalByStop.put(sourceStopId, departureTimeInSeconds);

        Map<Integer, Integer> previousRoundArrivalByStop = new HashMap<>();
        previousRoundArrivalByStop.put(sourceStopId, departureTimeInSeconds);

        Map<Integer, PathStep> previousRoundStepByStop = new HashMap<>();
        previousRoundStepByStop.put(sourceStopId, new SourceStep(sourceStopId, departureTimeInSeconds));

        Set<Integer> markedStops = new LinkedHashSet<>();
        markedStops.add(sourceStopId);

        List<RaptorJourney> journeys = new ArrayList<>();
        int maxRounds = Math.max(1, dataSet.stopsById().size());

        for (int round = 1; round <= maxRounds && !markedStops.isEmpty(); round++) {
            int bestTargetBeforeRound = arrivalAt(bestArrivalByStop, targetStopId);

            Map<Integer, Integer> currentRoundArrivalByStop = new HashMap<>(previousRoundArrivalByStop);
            Map<Integer, PathStep> currentRoundStepByStop = new HashMap<>(previousRoundStepByStop);
            Set<Integer> currentMarkedStops = new LinkedHashSet<>();

            Map<Integer, Integer> routesToScan = collectRoutesToScan(dataSet, markedStops);
            for (Map.Entry<Integer, Integer> routeEntry : routesToScan.entrySet()) {
                RouteRaptor route = dataSet.routesById().get(routeEntry.getKey());
                if (route == null) {
                    continue;
                }

                scanRoute(
                        dataSet,
                        route,
                        routeEntry.getValue(),
                        previousRoundArrivalByStop,
                        previousRoundStepByStop,
                        currentRoundArrivalByStop,
                        currentRoundStepByStop,
                        bestArrivalByStop,
                        currentMarkedStops,
                        targetStopId
                );
            }

            propagateTransfers(
                    dataSet,
                    markedStops,
                    currentMarkedStops,
                    currentRoundArrivalByStop,
                    currentRoundStepByStop,
                    bestArrivalByStop,
                    targetStopId
            );

            int bestTargetAfterRound = arrivalAt(bestArrivalByStop, targetStopId);
            if (bestTargetAfterRound < bestTargetBeforeRound) {
                PathStep targetStep = currentRoundStepByStop.get(targetStopId);
                if (targetStep != null) {
                    journeys.add(toJourney(
                            dataSet,
                            sourceStopId,
                            targetStopId,
                            departureTime,
                            targetStep
                    ));
                }
            }

            previousRoundArrivalByStop = currentRoundArrivalByStop;
            previousRoundStepByStop = currentRoundStepByStop;
            markedStops = currentMarkedStops;
        }

        return List.copyOf(journeys);
    }

    private void scanRoute(
            RaptorDataSet dataSet,
            RouteRaptor route,
            int startIndex,
            Map<Integer, Integer> previousRoundArrivalByStop,
            Map<Integer, PathStep> previousRoundStepByStop,
            Map<Integer, Integer> currentRoundArrivalByStop,
            Map<Integer, PathStep> currentRoundStepByStop,
            Map<Integer, Integer> bestArrivalByStop,
            Set<Integer> currentMarkedStops,
            int targetStopId
    ) {
        int currentTripIndex = -1;
        int boardedStopId = -1;
        int boardedStopIndex = -1;
        PathStep boardedParentStep = null;

        for (int stopIndex = startIndex; stopIndex < route.stopIds().length; stopIndex++) {
            int stopId = route.stopIds()[stopIndex];

            int previousRoundArrival = arrivalAt(previousRoundArrivalByStop, stopId);
            if (previousRoundArrival < UNREACHED) {
                int candidateTripIndex = findEarliestCatchableTrip(route.trips(), stopIndex, previousRoundArrival);
                if (candidateTripIndex >= 0 && isBetterBoardingChoice(route, stopIndex, currentTripIndex, candidateTripIndex)) {
                    currentTripIndex = candidateTripIndex;
                    boardedStopId = stopId;
                    boardedStopIndex = stopIndex;
                    boardedParentStep = previousRoundStepByStop.get(stopId);
                }
            }

            if (currentTripIndex < 0 || boardedParentStep == null || boardedStopId == stopId) {
                continue;
            }

            TripRaptor currentTrip = route.trips().get(currentTripIndex);
            int arrivalTime = currentTrip.arrivalTimes()[stopIndex];
            registerImprovement(
                    dataSet,
                    targetStopId,
                    stopId,
                    arrivalTime,
                    currentRoundArrivalByStop,
                    currentRoundStepByStop,
                    bestArrivalByStop,
                    currentMarkedStops,
                    new RideStep(
                            boardedParentStep,
                            stopId,
                            arrivalTime,
                            boardedStopId,
                            currentTrip.departureTimes()[boardedStopIndex],
                            route.sourceRouteId(),
                            currentTrip.id(),
                            route.headsign()
                    )
            );
        }
    }

    private void propagateTransfers(
            RaptorDataSet dataSet,
            Set<Integer> previouslyMarkedStops,
            Set<Integer> currentMarkedStops,
            Map<Integer, Integer> currentRoundArrivalByStop,
            Map<Integer, PathStep> currentRoundStepByStop,
            Map<Integer, Integer> bestArrivalByStop,
            int targetStopId
    ) {
        Queue<Integer> queue = new ArrayDeque<>();

        enqueueAll(previouslyMarkedStops, queue);
        enqueueAll(currentMarkedStops, queue);

        while (!queue.isEmpty()) {
            int stopId = queue.poll();
            StopRaptor stop = dataSet.stopsById().get(stopId);
            PathStep parentStep = currentRoundStepByStop.get(stopId);
            int stopArrivalTime = arrivalAt(currentRoundArrivalByStop, stopId);

            if (stop == null || parentStep == null || stopArrivalTime == UNREACHED) {
                continue;
            }

            for (TransferRaptor transfer : stop.transfers()) {
                int destinationStopId = transfer.destinationStopId();
                int destinationArrivalTime = stopArrivalTime + transfer.durationInSeconds();

                boolean improved = registerImprovement(
                        dataSet,
                        targetStopId,
                        destinationStopId,
                        destinationArrivalTime,
                        currentRoundArrivalByStop,
                        currentRoundStepByStop,
                        bestArrivalByStop,
                        currentMarkedStops,
                        new TransferStep(
                                parentStep,
                                destinationStopId,
                                destinationArrivalTime,
                                stopId,
                                stopArrivalTime
                        )
                );

                if (improved) {
                    queue.add(destinationStopId);
                }
            }
        }
    }

    private boolean registerImprovement(
            RaptorDataSet dataSet,
            int targetStopId,
            int stopId,
            int candidateArrivalTime,
            Map<Integer, Integer> currentRoundArrivalByStop,
            Map<Integer, PathStep> currentRoundStepByStop,
            Map<Integer, Integer> bestArrivalByStop,
            Set<Integer> currentMarkedStops,
            PathStep step
    ) {
        int currentArrival = arrivalAt(currentRoundArrivalByStop, stopId);
        if (candidateArrivalTime >= currentArrival) {
            return false;
        }

        int bestKnownTargetArrival = arrivalAt(bestArrivalByStop, targetStopId);
        if (stopId != targetStopId && candidateArrivalTime >= bestKnownTargetArrival) {
            return false;
        }

        currentRoundArrivalByStop.put(stopId, candidateArrivalTime);
        currentRoundStepByStop.put(stopId, step);

        int bestArrival = arrivalAt(bestArrivalByStop, stopId);
        if (candidateArrivalTime < bestArrival) {
            bestArrivalByStop.put(stopId, candidateArrivalTime);
            currentMarkedStops.add(stopId);
        }

        return true;
    }

    private Map<Integer, Integer> collectRoutesToScan(RaptorDataSet dataSet, Set<Integer> markedStops) {
        Map<Integer, Integer> routesToScan = new LinkedHashMap<>();

        for (int stopId : markedStops) {
            StopRaptor stop = dataSet.stopsById().get(stopId);
            if (stop == null) {
                continue;
            }

            for (RouteAtStopRaptor routeAtStop : stop.routes()) {
                routesToScan.merge(
                        routeAtStop.routeId(),
                        routeAtStop.stopIndexInRoute(),
                        Math::min
                );
            }
        }

        return routesToScan;
    }

    private int findEarliestCatchableTrip(List<TripRaptor> trips, int stopIndex, int earliestDepartureTime) {
        for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
            if (trips.get(tripIndex).departureTimes()[stopIndex] >= earliestDepartureTime) {
                return tripIndex;
            }
        }

        return -1;
    }

    private boolean isBetterBoardingChoice(
            RouteRaptor route,
            int stopIndex,
            int currentTripIndex,
            int candidateTripIndex
    ) {
        if (currentTripIndex < 0) {
            return true;
        }

        int currentDeparture = route.trips().get(currentTripIndex).departureTimes()[stopIndex];
        int candidateDeparture = route.trips().get(candidateTripIndex).departureTimes()[stopIndex];

        return candidateDeparture < currentDeparture;
    }

    private RaptorJourney toJourney(
            RaptorDataSet dataSet,
            int sourceStopId,
            int targetStopId,
            LocalTime requestedDepartureTime,
            PathStep targetStep
    ) {
        List<RaptorJourneyLeg> legs = new ArrayList<>();
        PathStep current = targetStep;

        while (current != null && !(current instanceof SourceStep)) {
            if (current instanceof RideStep rideStep) {
                legs.add(new RaptorJourneyLeg(
                        JourneyLegType.RIDE,
                        rideStep.boardedStopId(),
                        stopName(dataSet, rideStep.boardedStopId()),
                        rideStep.stopId(),
                        stopName(dataSet, rideStep.stopId()),
                        toLocalTime(rideStep.boardingDepartureTime()),
                        toLocalTime(rideStep.arrivalTime()),
                        rideStep.sourceRouteId(),
                        rideStep.tripId(),
                        rideStep.headsign()
                ));
            } else if (current instanceof TransferStep transferStep) {
                legs.add(new RaptorJourneyLeg(
                        JourneyLegType.TRANSFER,
                        transferStep.fromStopId(),
                        stopName(dataSet, transferStep.fromStopId()),
                        transferStep.stopId(),
                        stopName(dataSet, transferStep.stopId()),
                        toLocalTime(transferStep.departureTime()),
                        toLocalTime(transferStep.arrivalTime()),
                        null,
                        null,
                        null
                ));
            }

            current = current.parent();
        }

        Collections.reverse(legs);

        int tripCount = (int) legs.stream()
                .filter(leg -> leg.type() == JourneyLegType.RIDE)
                .count();

        return new RaptorJourney(
                sourceStopId,
                targetStopId,
                requestedDepartureTime,
                toLocalTime(targetStep.arrivalTime()),
                tripCount,
                Math.max(0, tripCount - 1),
                List.copyOf(legs)
        );
    }

    private void enqueueAll(Set<Integer> stops, Queue<Integer> queue) {
        for (int stopId : stops) {
            queue.add(stopId);
        }
    }

    private String stopName(RaptorDataSet dataSet, int stopId) {
        StopRaptor stop = dataSet.stopsById().get(stopId);
        return stop == null ? null : stop.name();
    }

    private int arrivalAt(Map<Integer, Integer> arrivalByStop, int stopId) {
        return arrivalByStop.getOrDefault(stopId, UNREACHED);
    }

    private LocalTime toLocalTime(int seconds) {
        return LocalTime.ofSecondOfDay(Math.floorMod(seconds, SECONDS_PER_DAY));
    }

    private void validateStops(RaptorDataSet dataSet, int sourceStopId, int targetStopId) {
        if (!dataSet.stopsById().containsKey(sourceStopId)) {
            throw new IllegalArgumentException("Unknown source stop id: " + sourceStopId);
        }

        if (!dataSet.stopsById().containsKey(targetStopId)) {
            throw new IllegalArgumentException("Unknown target stop id: " + targetStopId);
        }
    }

    private sealed interface PathStep permits SourceStep, RideStep, TransferStep {
        PathStep parent();

        int stopId();

        int arrivalTime();
    }

    private record SourceStep(
            int stopId,
            int arrivalTime
    ) implements PathStep {

        @Override
        public PathStep parent() {
            return null;
        }
    }

    private record RideStep(
            PathStep parent,
            int stopId,
            int arrivalTime,
            int boardedStopId,
            int boardingDepartureTime,
            Long sourceRouteId,
            int tripId,
            String headsign
    ) implements PathStep {
    }

    private record TransferStep(
            PathStep parent,
            int stopId,
            int arrivalTime,
            int fromStopId,
            int departureTime
    ) implements PathStep {
    }
}
