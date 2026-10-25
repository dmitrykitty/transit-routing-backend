package com.dnikitin.transit.domain.service;

import com.dnikitin.transit.domain.model.raptor.JourneyLegType;
import com.dnikitin.transit.domain.model.raptor.RaptorJourney;
import com.dnikitin.transit.domain.model.raptor.RaptorJourneyLeg;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that a returned journey is executable on the network: legs are connected, every ride matches
 * the timetable of a trip that serves both stops in order, every walk is an existing footpath, and the
 * summary fields agree with the legs. Returns the problems found (empty when valid).
 */
final class JourneyValidator {

    private JourneyValidator() {
    }

    static List<String> problems(TestNetwork network, RaptorJourney journey, int source, int target, LocalTime departure) {
        List<String> problems = new ArrayList<>();
        int position = source;
        int time = departure.toSecondOfDay();
        int rides = 0;

        for (RaptorJourneyLeg leg : journey.legs()) {
            if (leg.fromStopId() != position) {
                problems.add("leg starts at " + leg.fromStopId() + " but traveller is at " + position);
            }
            int legDeparture = leg.departureTime().toSecondOfDay();
            int legArrival = leg.arrivalTime().toSecondOfDay();
            if (legDeparture < time) {
                problems.add("leg departs " + leg.departureTime() + " before traveller arrives (" + LocalTime.ofSecondOfDay(time) + ")");
            }

            if (leg.type() == JourneyLegType.RIDE) {
                rides++;
                if (!rideMatchesTimetable(network, leg, legDeparture, legArrival)) {
                    problems.add("ride " + leg + " does not match any trip timetable");
                }
            } else if (network.footpaths.stream().noneMatch(footpath -> footpath.fromStopId() == leg.fromStopId()
                    && footpath.toStopId() == leg.toStopId()
                    && footpath.durationSeconds() == legArrival - legDeparture)) {
                problems.add("walk " + leg + " is not a footpath of the network");
            }

            position = leg.toStopId();
            time = legArrival;
        }

        if (!journey.legs().isEmpty() && position != target) {
            problems.add("journey ends at " + position + " instead of " + target);
        }
        if (!journey.legs().isEmpty() && journey.arrivalTime().toSecondOfDay() != time) {
            problems.add("arrivalTime " + journey.arrivalTime() + " differs from last leg arrival " + LocalTime.ofSecondOfDay(time));
        }
        if (journey.tripCount() != rides) {
            problems.add("tripCount " + journey.tripCount() + " but " + rides + " ride legs");
        }
        return problems;
    }

    private static boolean rideMatchesTimetable(TestNetwork network, RaptorJourneyLeg leg, int departure, int arrival) {
        for (TestNetwork.Route route : network.routes) {
            for (TestNetwork.Trip trip : route.trips()) {
                if (leg.tripId() == null || trip.id() != leg.tripId()) {
                    continue;
                }
                for (int i = 0; i < route.stopIds().length; i++) {
                    for (int j = i + 1; j < route.stopIds().length; j++) {
                        if (route.stopIds()[i] == leg.fromStopId() && route.stopIds()[j] == leg.toStopId()
                                && trip.departures()[i] == departure && trip.arrivals()[j] == arrival) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
