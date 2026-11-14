package com.liftoff.engine.score;

import java.util.Map;


public final class ReportTexts {

    private static final Map<String, String> PATTERNS = Map.ofEntries(
            Map.entry(ReportBuilder.WORKED_MEASUREMENTS, "Your instruments can measure everything the mission needs."),
            Map.entry(ReportBuilder.WORKED_LAUNCH, "The spacecraft passed every launch check."),
            Map.entry(ReportBuilder.WORKED_DATA_RETURNED,
                    "{0,number,#.#} of the {1,number,#.#} gigabits you collected came home."),
            Map.entry(ReportBuilder.WORKED_NO_DATA_LOST, "No data was lost."),
            Map.entry(ReportBuilder.WORKED_POWER_ENOUGH, "There was always enough power for the instruments."),

            Map.entry(ReportBuilder.BROKE_NOTHING, "Nothing broke. Well flown!"),
            Map.entry(ReportBuilder.BROKE_REQUIREMENT_MISSING, "No instrument on board can measure {0}."),
            Map.entry(ReportBuilder.BROKE_REQUIREMENT_TOO_COARSE,
                    "The mission needs {0} with details of {1,number,#.#} m or finer, "
                            + "but your best instrument sees {2,number,#.#} m."),
            Map.entry(ReportBuilder.BROKE_MASS_OVER_CAPACITY,
                    "The spacecraft weighs {0,number,#.#} kg but the rocket can lift only {1,number,#.#} kg."),
            Map.entry(ReportBuilder.BROKE_ROCKET_NOT_OFFERED, "This rocket has no published capacity for this trip."),
            Map.entry(ReportBuilder.BROKE_FUEL_SHORT,
                    "The trip needs a speed change of {0,number,#} m/s but the ship can make only {1,number,#} m/s."),
            Map.entry(ReportBuilder.BROKE_ORBIT_CANNOT_REACH_TARGET,
                    "The target is at latitude {0,number,#.#} but the orbit only reaches {1,number,#.#}."),
            Map.entry(ReportBuilder.BROKE_OVER_BUDGET,
                    "The mission costs ${0,number,#.#}M but the budget is ${1,number,#.#}M."),
            Map.entry(ReportBuilder.BROKE_OVER_DEADLINE,
                    "The mission takes {0,number,#} months but the deadline is {1,number,#} months."),
            Map.entry(ReportBuilder.BROKE_POWER,
                    "In {1} {0} the instruments needed {2,number,#.#} W more than the spacecraft could give, "
                            + "so they were switched off part of the time."),
            Map.entry(ReportBuilder.BROKE_DOWNLINK,
                    "In {1} {0} there was not enough contact time: {2,number,#.#} gigabits were left waiting on board."),
            Map.entry(ReportBuilder.BROKE_STORAGE,
                    "In {1} {0} the data recorder filled up and {2,number,#.#} gigabits were lost."),
            Map.entry(ReportBuilder.BROKE_SUPPLIES, "In {1} {0} the crew ran out of supplies."),
            Map.entry(ReportBuilder.BROKE_NO_DATA_RETURNED, "The flight returned no data."),

            Map.entry(ReportBuilder.TRY_THINNEST_MARGIN,
                    "Your thinnest margin was {0}: {1,number,#.#} of {2,number,#.#} {3}. Keep an eye on it."),
            Map.entry(ReportBuilder.TRY_HARDER_MISSION, "Try a harder mission."),
            Map.entry(ReportBuilder.TRY_ADD_INSTRUMENT_FOR, "Add an instrument that can measure {0}."),
            Map.entry(ReportBuilder.TRY_SHARPER_INSTRUMENT,
                    "Choose an instrument that sees {0} at {1,number,#.#} m or finer."),
            Map.entry(ReportBuilder.TRY_BIGGER_ROCKET_OR_LIGHTER_SHIP,
                    "Choose a rocket that lifts at least {0,number,#.#} kg, or make the spacecraft lighter."),
            Map.entry(ReportBuilder.TRY_OTHER_ROCKET, "Choose a rocket that is offered for this trip."),
            Map.entry(ReportBuilder.TRY_MORE_FUEL,
                    "Choose a body with bigger tanks or a more efficient thruster, or make the ship lighter."),
            Map.entry(ReportBuilder.TRY_STEEPER_ORBIT,
                    "Choose an orbit that reaches latitude {0,number,#.#} or higher."),
            Map.entry(ReportBuilder.TRY_CHEAPER_PARTS,
                    "Save ${0,number,#.#}M: choose cheaper parts, fewer stations or a shorter science time."),
            Map.entry(ReportBuilder.TRY_SHORTER_SCIENCE_OR_FASTER_PARTS,
                    "Save {0,number,#} months: shorten the science time or choose parts that are quicker to build."),
            Map.entry(ReportBuilder.TRY_MORE_POWER_OR_FEWER_INSTRUMENTS,
                    "Find {0,number,#.#} W: choose a stronger power source or carry fewer instruments."),
            Map.entry(ReportBuilder.TRY_ADD_STATION, "Add a ground station so the data can come home."),
            Map.entry(ReportBuilder.TRY_ADD_STATION_OR_FASTER_RADIO,
                    "Add another ground station, or choose a spacecraft with a faster radio."),
            Map.entry(ReportBuilder.TRY_MORE_SUPPLIES_OR_SHORTER_STAY, "Pack more supplies, or plan a shorter stay."),
            Map.entry(ReportBuilder.TRY_ADD_INSTRUMENT, "Add an instrument so there is something to measure."));

    private ReportTexts() {
    }


    public static String pattern(String messageKey) {
        return PATTERNS.get(messageKey);
    }
}
