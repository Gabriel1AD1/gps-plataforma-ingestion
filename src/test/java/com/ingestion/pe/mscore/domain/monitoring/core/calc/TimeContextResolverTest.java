package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import static org.junit.jupiter.api.Assertions.*;

import com.ingestion.pe.mscore.domain.monitoring.core.model.TimeSpanModel;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimeContextResolverTest {

    private final TimeContextResolver resolver = new TimeContextResolver();

    private TimeSpanModel span(Long id, String name, int dayMask, LocalTime start, LocalTime end) {
        return TimeSpanModel.builder()
                .id(id).name(name)
                .dayOfWeekMask(dayMask)
                .startTime(start).endTime(end)
                .build();
    }

    @Test
    void resolveCurrentTimeSpan_nullList_returnsEmpty() {
        assertTrue(resolver.resolveCurrentTimeSpan(null, Instant.now()).isEmpty());
    }

    @Test
    void resolveCurrentTimeSpan_emptyList_returnsEmpty() {
        assertTrue(resolver.resolveCurrentTimeSpan(List.of(), Instant.now()).isEmpty());
    }

    @Test
    void resolveCurrentTimeSpan_matchingSpan_returnsIt() {
        Instant mondayAt10 = ZonedDateTime.of(2026, 3, 2, 10, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        int mondayMask = 1; // bit 0

        TimeSpanModel morningSpan = span(1L, "Mañana", mondayMask, LocalTime.of(6, 0), LocalTime.of(12, 0));

        Optional<TimeSpanModel> result = resolver.resolveCurrentTimeSpan(List.of(morningSpan), mondayAt10);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void resolveCurrentTimeSpan_outsideHours_returnsEmpty() {
        Instant mondayAt15 = ZonedDateTime.of(2026, 3, 2, 15, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        int mondayMask = 1;

        TimeSpanModel morningSpan = span(1L, "Mañana", mondayMask, LocalTime.of(6, 0), LocalTime.of(12, 0));

        Optional<TimeSpanModel> result = resolver.resolveCurrentTimeSpan(List.of(morningSpan), mondayAt15);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCurrentTimeSpan_wrongDay_returnsEmpty() {
        Instant tuesdayAt10 = ZonedDateTime.of(2026, 3, 3, 10, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        int mondayOnlyMask = 1;

        TimeSpanModel morningSpan = span(1L, "Mañana", mondayOnlyMask, LocalTime.of(6, 0), LocalTime.of(12, 0));

        assertTrue(resolver.resolveCurrentTimeSpan(List.of(morningSpan), tuesdayAt10).isEmpty());
    }

    @Test
    void resolveCurrentTimeSpan_multipleSpans_returnsFirstMatch() {
        Instant mondayAt10 = ZonedDateTime.of(2026, 3, 2, 10, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        int allDaysMask = 127;

        TimeSpanModel morningSpan = span(1L, "Mañana", allDaysMask, LocalTime.of(6, 0), LocalTime.of(12, 0));
        TimeSpanModel fullDaySpan = span(2L, "Completo", allDaysMask, LocalTime.of(0, 0), LocalTime.of(23, 59));

        Optional<TimeSpanModel> result = resolver.resolveCurrentTimeSpan(
                List.of(morningSpan, fullDaySpan), mondayAt10);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId(), "Debe retornar el PRIMER match");
    }
}
