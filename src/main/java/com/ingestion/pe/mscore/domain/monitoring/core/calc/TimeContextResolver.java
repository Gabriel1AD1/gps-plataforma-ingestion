package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeSpanModel;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TimeContextResolver {

    public Optional<TimeSpanModel> resolveCurrentTimeSpan(List<TimeSpanModel> timeSpans, Instant now) {
        if (timeSpans == null || timeSpans.isEmpty()) {
            return Optional.empty();
        }

        LocalTime currentTime = now.atZone(ZoneId.systemDefault()).toLocalTime();
        int todayMask = 1 << (now.atZone(ZoneId.systemDefault()).getDayOfWeek().getValue() - 1);

        return timeSpans.stream()
                .filter(ts -> ts.getDayOfWeekMask() != null && (ts.getDayOfWeekMask() & todayMask) != 0)
                .filter(ts -> ts.getStartTime() != null && ts.getEndTime() != null)
                .filter(ts -> !currentTime.isBefore(ts.getStartTime()) && !currentTime.isAfter(ts.getEndTime()))
                .findFirst();
    }
}
