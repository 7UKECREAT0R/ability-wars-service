package org.lukecreator.aw.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * A generic Discord bot command.
 */
public abstract class BotCommand {
    private static final Pattern userMentionPattern = Message.MentionType.USER.getPattern();
    private static final Pattern everyonePattern = Message.MentionType.EVERYONE.getPattern();
    private static final Pattern herePattern = Message.MentionType.HERE.getPattern();
    private static final Pattern rolePattern = Message.MentionType.ROLE.getPattern();
    /**
     * The name of this command; i.e., what the user has to type to trigger it.
     */
    public final String name;
    /**
     * The description of the command.
     */
    public final String description;

    /**
     * Create a new Discord bot command.
     *
     * @param name        The name of the command. 1-32 lowercase alphanumeric characters.
     * @param description The description of the command. 1-100 characters.
     */
    public BotCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Calculates a human-readable string describing the duration between two timestamps.
     * If the start timestamp is after the end timestamp, it will return "in the future".
     * Otherwise, computes how many days, months, or years ago the start timestamp occurred
     * relative to the end timestamp and returns a corresponding description.
     *
     * @param start The starting timestamp in milliseconds since the epoch.
     * @param end   The ending timestamp in milliseconds since the epoch.
     * @return A string describing the duration between the two timestamps, such as
     * "today", "1 day ago", "2 months ago", or "3 years ago".
     */
    @NotNull
    public static String getDurationDescriptor(long start, long end) {
        if (start > end)
            return "in the future";

        long startDays = start / (1000 * 60 * 60 * 24);
        long endDays = end / (1000 * 60 * 60 * 24);

        int daysAgo = (int) (endDays - startDays);
        if (daysAgo < 30) {
            return switch (daysAgo) {
                case 0 -> "today";
                case 1 -> "1 day ago";
                default -> daysAgo + " days ago";
            };
        } else if (daysAgo < 365) {
            int monthsAgo = daysAgo / 30;
            return monthsAgo == 1 ? "1 month ago" : monthsAgo + " months ago";
        } else {
            int yearsAgo = daysAgo / 365;
            return yearsAgo == 1 ? "1 year ago" : yearsAgo + " years ago";
        }
    }

    /**
     * Provides a descriptive error message when the input does not correspond to a discoverable Roblox user.
     * This method analyzes the input and generates a message based on its format, such as Discord mentions,
     * roles, Roblox IDs, or usernames.
     *
     * @param input The input string to analyze, which could be a Discord mention, role, Roblox ID, or Roblox username.
     * @return A descriptive error message explaining why the input could not be resolved to a Roblox user.
     */
    @NotNull
    public static String getUnknownUsernameDescriptor(String input) {
        input = input.replace("`", "");

        // malicious case
        if (everyonePattern.matcher(input).find() ||
                herePattern.matcher(input).find() ||
                rolePattern.matcher(input).find()) {
            return "Can't lookup a Roblox user based on a role. Try putting their Roblox username or ID there instead.";
        }

        // discord mention
        var matcher = userMentionPattern.matcher(input);
        if (matcher.matches()) {
            // unwrap ping into a Discord ID and then try to find a link
            long discordId = Long.parseLong(matcher.group(1));
            return "Couldn't obtain the Roblox account of the Discord user <@" + discordId + ">. Try using their Roblox username instead.";
        }

        // roblox ID
        try {
            long id = Long.parseLong(input);
            return "Can't find any Roblox account with the ID `" + id + "`.";
        } catch (NumberFormatException ignored) {
            // roblox username
            return "Can't find any Roblox account with the username `" + input + "`.";
        }
    }

    /**
     * Construct a Discord Slash Command for this command implementation.
     *
     * @return A {@link SlashCommandData} that represents this command's syntax, etc..
     */
    public abstract SlashCommandData constructCommand();

    public abstract void execute(SlashCommandInteractionEvent e) throws SQLException;
}
