package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.maintenance.application.MaintenanceTicket;
import com.werkpilot.maintenance.application.MaintenanceTicketPort;
import com.werkpilot.maintenance.application.MaintenanceTicketService;
import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketOverdueCalculationTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-20T08:15:00Z"),
            ZoneId.of("UTC"));
    private static final LocalDate CURRENT_VIENNA_BUSINESS_DATE = LocalDate.of(2026, 7, 20);

    private final MaintenanceTicketService service = serviceWith(FIXED_CLOCK);

    @Test
    void dueDateBeforeCurrentViennaBusinessDateIsOverdueForOpenAndInProgress() {
        LocalDate yesterday = CURRENT_VIENNA_BUSINESS_DATE.minusDays(1);

        assertThat(service.overdue(ticket(TicketStatus.OPEN, yesterday))).isTrue();
        assertThat(service.overdue(ticket(TicketStatus.IN_PROGRESS, yesterday))).isTrue();
    }

    @Test
    void dueDateEqualToCurrentViennaBusinessDateIsNotOverdue() {
        assertThat(service.overdue(ticket(TicketStatus.OPEN, CURRENT_VIENNA_BUSINESS_DATE))).isFalse();
        assertThat(service.overdue(ticket(TicketStatus.IN_PROGRESS, CURRENT_VIENNA_BUSINESS_DATE))).isFalse();
    }

    @Test
    void dueDateAfterCurrentViennaBusinessDateIsNotOverdue() {
        LocalDate tomorrow = CURRENT_VIENNA_BUSINESS_DATE.plusDays(1);

        assertThat(service.overdue(ticket(TicketStatus.OPEN, tomorrow))).isFalse();
        assertThat(service.overdue(ticket(TicketStatus.IN_PROGRESS, tomorrow))).isFalse();
    }

    @Test
    void nullDueDateIsNotOverdue() {
        assertThat(service.overdue(ticket(TicketStatus.OPEN, null))).isFalse();
        assertThat(service.overdue(ticket(TicketStatus.IN_PROGRESS, null))).isFalse();
    }

    @Test
    void resolvedAndCancelledAreNeverOverdue() {
        LocalDate yesterday = CURRENT_VIENNA_BUSINESS_DATE.minusDays(1);

        assertThat(service.overdue(ticket(TicketStatus.RESOLVED, yesterday))).isFalse();
        assertThat(service.overdue(ticket(TicketStatus.CANCELLED, yesterday))).isFalse();
    }

    @Test
    void overdueIsComputedAndNeverPersistedAsTicketStatus() {
        MaintenanceTicket overdueTicket = ticket(TicketStatus.OPEN, CURRENT_VIENNA_BUSINESS_DATE.minusDays(1));

        assertThat(service.overdue(overdueTicket)).isTrue();
        assertThat(overdueTicket.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(Arrays.stream(TicketStatus.values()).map(Enum::name))
                .doesNotContain("OVERDUE");
    }

    private static MaintenanceTicketService serviceWith(Clock clock) {
        try {
            Constructor<MaintenanceTicketService> constructor = MaintenanceTicketService.class.getDeclaredConstructor(
                    MaintenanceTicketPort.class,
                    AuditEventPort.class,
                    Clock.class);
            constructor.setAccessible(true);
            return constructor.newInstance(mock(MaintenanceTicketPort.class), mock(AuditEventPort.class), clock);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not construct MaintenanceTicketService with fixed Clock.", ex);
        }
    }

    private static MaintenanceTicket ticket(TicketStatus status, LocalDate dueDate) {
        Instant now = Instant.parse("2026-07-20T08:15:00Z");
        return new MaintenanceTicket(
                UUID.randomUUID(),
                "Ticket",
                "Description",
                status,
                TicketPriority.MEDIUM,
                null,
                null,
                null,
                null,
                null,
                dueDate,
                status == TicketStatus.RESOLVED ? "Resolved." : null,
                status == TicketStatus.CANCELLED ? "Cancelled." : null,
                UUID.randomUUID(),
                now,
                now);
    }
}
