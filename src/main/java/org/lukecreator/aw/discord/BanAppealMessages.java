package org.lukecreator.aw.discord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Generates the ban appeal message shown to reported users, and holds the quick-pick exploit
 * names moderators use most often. Mirrors the reason/appeal-date template from the moderation
 * team's desktop ban message macro.
 */
public class BanAppealMessages {
    public static final List<String> QUICK_REASONS = List.of(
            "Flying", "Speed", "Teleport", "Autofarm", "Flinging",
            "Follow Script", "Jump Power", "Antivoid", "Reach", "Offensive Engineer Builds"
    );

    private static final String TEMPLATE =
            "%s based on video proof submitted to the moderation team. " +
                    "You may appeal on or after %s. If you believe the evidence is incorrect, or if this was a bug, " +
                    "you may dispute it any time or request to review the video. To appeal or review evidence, use the social links below the game page " +
                    "and please be honest.";

    private static final DateTimeFormatter APPEAL_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    /**
     * Builds the ban appeal message for the given reason, with the appeal date set six months from today.
     */
    public static String generate(String reason) {
        LocalDate appealDate = LocalDate.now().plusMonths(6);
        return TEMPLATE.formatted(reason.strip().toUpperCase(Locale.ENGLISH), APPEAL_DATE_FORMAT.format(appealDate));
    }
}
