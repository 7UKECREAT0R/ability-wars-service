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

    public static final List<String> QUICK_TEMPBAN_REASONS = List.of(
            "Bug Abuse - Increased Speed", "Bug Abuse - High Jump", "Bug Abuse - Invincibility",
            "Bug Abuse - Permanent Invisibility", "Bug Abuse - Tab Glitching", "Bug Abuse - Low Gravity"
    );

    private static final String TEMPLATE =
            "The Following: %s based on evidence submitted to and reviewed by the moderation team. " +
                    "You may appeal on or after %s. If you believe this action was made in error, you may submit a dispute at any time. " +
                    "The moderation team will review your claim before making a final decision. To submit a dispute or appeal, use the social links below the game page, " +
                    "and please be honest.";

    private static final DateTimeFormatter APPEAL_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private static final String BUG_ABUSE_TEMPLATE = "The Following: %s. If you are affected by a bug in the future, please reset and don't abuse it.";

    /**
     * Builds the ban appeal message for the given reason, with the appeal date set six months from today.
     */
    public static String generate(String reason) {
        LocalDate appealDate = LocalDate.now().plusMonths(6);
        return TEMPLATE.formatted(reason.strip().toUpperCase(Locale.ENGLISH), APPEAL_DATE_FORMAT.format(appealDate));
    }

    /**
     * Builds the tempban message shown for a quick-pick bug-abuse reason.
     */
    public static String generateBugAbuseMessage(String reason) {
        return BUG_ABUSE_TEMPLATE.formatted(reason.strip().toUpperCase(Locale.ENGLISH));
    }
}
