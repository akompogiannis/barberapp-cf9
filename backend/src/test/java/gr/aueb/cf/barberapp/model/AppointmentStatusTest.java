package gr.aueb.cf.barberapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static gr.aueb.cf.barberapp.model.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @Test
    @DisplayName("only pending and confirmed appointments occupy a slot")
    void onlyLiveStatusesBlockSlots() {
        assertThat(PENDING.blocksSlot()).isTrue();
        assertThat(CONFIRMED.blocksSlot()).isTrue();

        // These free the slot again, which is what lets a cancellation be rebooked.
        assertThat(CANCELLED.blocksSlot()).isFalse();
        assertThat(COMPLETED.blocksSlot()).isFalse();
        assertThat(NO_SHOW.blocksSlot()).isFalse();
    }

    @Test
    @DisplayName("a pending appointment can only be confirmed or cancelled")
    void pendingTransitions() {
        assertThat(PENDING.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(PENDING.canTransitionTo(CANCELLED)).isTrue();

        // Cannot be completed or marked a no-show before it was ever confirmed.
        assertThat(PENDING.canTransitionTo(COMPLETED)).isFalse();
        assertThat(PENDING.canTransitionTo(NO_SHOW)).isFalse();
        assertThat(PENDING.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    @DisplayName("a confirmed appointment can be completed, cancelled or marked a no-show")
    void confirmedTransitions() {
        assertThat(CONFIRMED.canTransitionTo(COMPLETED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(CANCELLED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(NO_SHOW)).isTrue();

        assertThat(CONFIRMED.canTransitionTo(PENDING)).isFalse();
        assertThat(CONFIRMED.canTransitionTo(CONFIRMED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    @DisplayName("terminal statuses cannot be moved anywhere")
    void terminalStatusesAreFinal(AppointmentStatus terminal) {
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(terminal.canTransitionTo(target))
                    .as("%s should not transition to %s", terminal, target)
                    .isFalse();
        }
    }
}
