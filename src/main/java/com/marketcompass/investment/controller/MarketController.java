package com.marketcompass.investment.controller;

import com.marketcompass.investment.model.MarketSession;
import com.marketcompass.investment.service.MarketClockService;
import com.marketcompass.investment.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Market simulation REST API.
 *
 * GET /api/market/status  — current session, ET time, agent commentary
 * GET /api/market/movers  — top price movers for the current session
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketClockService      clock;
    private final MarketSimulationService simulation;

    public MarketController(MarketClockService clock,
                            MarketSimulationService simulation) {
        this.clock      = clock;
        this.simulation = simulation;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        MarketSession session   = clock.getSession();
        String        direction = session == MarketSession.OPEN
                                  ? simulation.getMarketDirection() : "N/A";

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("session",         session.name());
        status.put("sessionLabel",    session.label());
        status.put("sessionBadge",    session.badge());
        status.put("marketOpen",      session == MarketSession.OPEN);
        status.put("currentTimeET",   clock.currentTimeFormatted());
        status.put("nextEventLabel",  clock.nextEventLabel());
        status.put("nextEventTime",   clock.nextEventTime());
        status.put("marketDirection", direction);
        status.put("agentComment",    buildAgentComment(session, direction));
        return ResponseEntity.ok(status);
    }

    @GetMapping("/movers")
    public ResponseEntity<?> getMovers() {
        return ResponseEntity.ok(simulation.getTopMovers(15));
    }

    // ─── Agent commentary ─────────────────────────────────────────────────────

    private String buildAgentComment(MarketSession session, String direction) {
        return switch (session) {
            case OPEN -> switch (direction) {
                case "UP"    -> "📈 Broad market is advancing. Risk appetite is healthy — growth and chip names are leading.";
                case "DOWN"  -> "📉 Selling pressure today. Defensive sectors are holding better than high-beta tech.";
                default      -> "↔️ Mixed session. Markets are digesting recent data with no clear directional bias.";
            };
            case PRE_MARKET  -> "🌅 Pre-market is active. Watch for overnight gaps driven by earnings or macro news before the bell.";
            case AFTER_HOURS -> "🌙 After-hours trading. Volume is thin — price moves can be exaggerated. Treat them with caution.";
            case CLOSED      -> "💤 Markets are closed. Use this time to research positions, not react to noise. Good decisions are made slowly.";
        };
    }
}
