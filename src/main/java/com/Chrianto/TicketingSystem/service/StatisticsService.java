package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.EntityShareResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketStatsPointResponse;
import com.Chrianto.TicketingSystem.entity.Subcategory;
import com.Chrianto.TicketingSystem.repository.CategoryRepository;
import com.Chrianto.TicketingSystem.repository.SubcategoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketHistoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final int DAILY_WINDOW_DAYS = 30;
    private static final int MONTHLY_WINDOW_MONTHS = 12;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    // Fixed 30-day window — not yet configurable from the UI.
    public List<TicketStatsPointResponse> getDailyStats() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(DAILY_WINDOW_DAYS - 1L);
        LocalDateTime from = start.atStartOfDay();

        Map<LocalDate, Long> created = toDayCountMap(ticketRepository.countCreatedByDay(from));
        Map<LocalDate, Long> resolved = toDayCountMap(ticketHistoryRepository.countActionByDay("RESOLVED", from));
        Map<LocalDate, Long> cancelled = toDayCountMap(ticketHistoryRepository.countActionByDay("CANCELLED", from));

        List<TicketStatsPointResponse> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            points.add(TicketStatsPointResponse.builder()
                    .period(d.toString())
                    .created(created.getOrDefault(d, 0L))
                    .resolved(resolved.getOrDefault(d, 0L))
                    .cancelled(cancelled.getOrDefault(d, 0L))
                    .build());
        }
        return points;
    }

    // Fixed 12-month window — not yet configurable from the UI.
    public List<TicketStatsPointResponse> getMonthlyStats() {
        YearMonth thisMonth = YearMonth.now();
        YearMonth start = thisMonth.minusMonths(MONTHLY_WINDOW_MONTHS - 1L);
        LocalDateTime from = start.atDay(1).atStartOfDay();

        Map<YearMonth, Long> created = toMonthCountMap(ticketRepository.countCreatedByMonth(from));
        Map<YearMonth, Long> resolved = toMonthCountMap(ticketHistoryRepository.countActionByMonth("RESOLVED", from));
        Map<YearMonth, Long> cancelled = toMonthCountMap(ticketHistoryRepository.countActionByMonth("CANCELLED", from));

        List<TicketStatsPointResponse> points = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(thisMonth); m = m.plusMonths(1)) {
            points.add(TicketStatsPointResponse.builder()
                    .period(m.format(MONTH_FORMAT))
                    .created(created.getOrDefault(m, 0L))
                    .resolved(resolved.getOrDefault(m, 0L))
                    .cancelled(cancelled.getOrDefault(m, 0L))
                    .build());
        }
        return points;
    }

    // Share of all-time ticket volume per category, any ticket status — matches
    // the ticketCount already shown on the Categories page (ticketRepository's
    // count query isn't status-filtered there either). Zero-ticket categories are
    // dropped rather than plotted as an empty bar.
    public List<EntityShareResponse> getCategoryShares() {
        Map<Long, Long> counts = ticketRepository.countTicketsGroupedByCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return categoryRepository.findAll().stream()
                .map(c -> toShare(c.getId(), c.getName(), counts.getOrDefault(c.getId(), 0L), total))
                .filter(r -> r.getTicketCount() > 0)
                .sorted(Comparator.comparingLong(EntityShareResponse::getTicketCount).reversed())
                .toList();
    }

    // Percentage here is relative to the subcategory's OWN category total (not
    // the global ticket count) — these are nested under each category's row on
    // the frontend now, so "share of this category" is the useful number, and
    // the values within one category's group sum to 100%.
    public List<EntityShareResponse> getSubcategoryShares() {
        Map<Long, Long> counts = ticketRepository.countTicketsGroupedBySubcategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<Subcategory> subcategories = subcategoryRepository.findAll();
        Map<Long, Long> categoryTotals = new HashMap<>();
        for (Subcategory s : subcategories) {
            categoryTotals.merge(s.getCategory().getId(), counts.getOrDefault(s.getId(), 0L), Long::sum);
        }

        return subcategories.stream()
                .map(s -> {
                    Long categoryId = s.getCategory().getId();
                    long count = counts.getOrDefault(s.getId(), 0L);
                    long categoryTotal = categoryTotals.getOrDefault(categoryId, 0L);
                    return EntityShareResponse.builder()
                            .id(s.getId())
                            .parentId(categoryId)
                            .name(s.getName())
                            .ticketCount(count)
                            .percentage(categoryTotal == 0 ? 0.0 : count * 100.0 / categoryTotal)
                            .build();
                })
                .filter(r -> r.getTicketCount() > 0)
                .sorted(Comparator.comparing(EntityShareResponse::getParentId)
                        .thenComparing(Comparator.comparingLong(EntityShareResponse::getTicketCount).reversed()))
                .toList();
    }

    private EntityShareResponse toShare(Long id, String name, long count, long total) {
        return EntityShareResponse.builder()
                .id(id)
                .name(name)
                .ticketCount(count)
                .percentage(total == 0 ? 0.0 : count * 100.0 / total)
                .build();
    }

    private Map<LocalDate, Long> toDayCountMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = toLocalDateTime(row[0]).toLocalDate();
            map.put(date, ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<YearMonth, Long> toMonthCountMap(List<Object[]> rows) {
        Map<YearMonth, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            YearMonth ym = YearMonth.from(toLocalDateTime(row[0]));
            map.put(ym, ((Number) row[1]).longValue());
        }
        return map;
    }

    // date_trunc() on a "timestamp without time zone" column comes back as a
    // LocalDateTime with this Hibernate/driver version — but that's an
    // implementation detail worth not trusting blindly, hence the Timestamp
    // fallback rather than a bare cast.
    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }
}
