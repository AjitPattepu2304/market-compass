package com.marketcompass.investment.service;

import com.marketcompass.investment.model.MarketSession;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Determines the current NYSE/NASDAQ session based on US Eastern time.
 *
 * Sessions (all ET):
 *   PRE_MARKET  — Mon–Fri, 4:00 AM → 9:29 AM
 *   OPEN        — Mon–Fri, 9:30 AM → 3:59 PM  (regular session)
 *   AFTER_HOURS — Mon–Fri, 4:00 PM → 7:59 PM
 *   CLOSED      — all other times + weekends
 */
@Service
public class MarketClockService {

    private static final ZoneId ET = ZoneId.of("America/New_York");

    private static final LocalTime PRE_OPEN   = LocalTime.of(4, 0);
    private static final LocalTime OPEN       = LocalTime.of(9, 30);
    private static final LocalTime CLOSE      = LocalTime.of(16, 0);
    private static final LocalTime AFTER_END  = LocalTime.of(20, 0);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a z");

    public ZonedDateTime nowET() {
        return ZonedDateTime.now(ET);
    }

    public MarketSession getSession() {
        ZonedDateTime now = nowET();
        DayOfWeek day = now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return MarketSession.CLOSED;
        }

        LocalTime t = now.toLocalTime();
        if (t.isBefore(PRE_OPEN))  return MarketSession.CLOSED;
        if (t.isBefore(OPEN))      return MarketSession.PRE_MARKET;
        if (t.isBefore(CLOSE))     return MarketSession.OPEN;
        if (t.isBefore(AFTER_END)) return MarketSession.AFTER_HOURS;
        return MarketSession.CLOSED;
    }

    public boolean isRegularSession() {
        return getSession() == MarketSession.OPEN;
    }

    /** Current ET time formatted as "9:45 AM ET". */
    public String currentTimeFormatted() {
        return nowET().format(TIME_FMT);
    }

    /** "Closes at" when OPEN, "Opens at" for all other sessions. */
    public String nextEventLabel() {
        return getSession() == MarketSession.OPEN ? "Closes at" : "Opens at";
    }

    /** Human-readable time for the next session boundary. */
    public String nextEventTime() {
        ZonedDateTime now = nowET();
        DayOfWeek day = now.getDayOfWeek();

        return switch (getSession()) {
            case OPEN        -> "4:00 PM ET";
            case PRE_MARKET  -> "9:30 AM ET";
            case AFTER_HOURS -> nextTradingDayLabel(day, "9:30 AM ET");
            case CLOSED      -> nextTradingDayLabel(day, "9:30 AM ET");
        };
    }

    private String nextTradingDayLabel(DayOfWeek day, String time) {
        String when = switch (day) {
            case FRIDAY, SATURDAY, SUNDAY -> " Mon";
            default -> " Tomorrow";
        };
        return time + when;
    }
}
