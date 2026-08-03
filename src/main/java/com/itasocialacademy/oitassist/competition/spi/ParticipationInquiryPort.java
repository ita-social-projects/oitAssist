package com.itasocialacademy.oitassist.competition.spi;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import org.springframework.modulith.NamedInterface;

/**
 * Port through which the {@code competition} module queries whether active
 * participants exist for a Competition or Stage. Implemented by the
 * {@code participation} module. This interface exists specifically to avoid a
 * cyclic module dependency: {@code participation} already depends on
 * {@code competition} via {@link CompetitionFacade}. By owning this port here
 * instead of depending on {@code participation.api}, {@code competition} has
 * zero compile-time dependency on {@code participation} — the dependency edge
 * points only one way.
 */
@NamedInterface("spi")
public interface ParticipationInquiryPort {
    boolean competitionHasParticipants(Long competitionId);

    boolean stageHasParticipants(Long stageId);
}
