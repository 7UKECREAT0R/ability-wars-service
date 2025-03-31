package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.data.AWBans;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class TicketCountCommand extends BotCommand {
    public TicketCountCommand() {
        super("ticketcount", "Count the number of tickets someone has done this week. (or last week)");
    }

    /**
     * Calculates the start of the week for a given timestamp.
     * The calculation assumes weeks start on Sunday at 00:00 in the UTC time zone.
     *
     * @param timestamp The timestamp in milliseconds since the epoch for which the week's start is to be calculated.
     * @return The timestamp in milliseconds since the epoch representing the start of the week.
     */
    public static long calculateWeekStart(long timestamp) {
        LocalDate now = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // 0 == Monday
        // 6 == Sunday
        int dayOfWeekValue = dayOfWeek.getValue() - 1;

        LocalDate startOfWeek = now.minusDays(dayOfWeekValue);
        return startOfWeek.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /**
     * Calculates the start of the week for a given timestamp and a specified number of weeks ago.
     * The calculation assumes weeks start on Monday at 00:00 in the UTC time zone.
     *
     * @param timestamp The timestamp in milliseconds since the epoch for which the week's start is to be calculated.
     * @param weeksAgo  The number of weeks to go back from the current week.
     * @return The timestamp in milliseconds since the epoch representing the start of the specified week.
     */
    public static long calculateWeekStart(long timestamp, int weeksAgo) {
        LocalDate now = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
                .minusWeeks(weeksAgo);
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // 0 == Monday
        // 6 == Sunday
        int dayOfWeekValue = dayOfWeek.getValue() - 1;

        LocalDate startOfWeek = now.minusDays(dayOfWeekValue);
        return startOfWeek.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static EmbedBuilder buildTicketCountEmbed(User staff, int ticketCount, boolean lastWeek, long timestampStart, long timestampEnd) {
        timestampStart /= 1000; // it's in seconds
        timestampEnd /= 1000; // it's in seconds

        String timeWindow = "Time period: " + (lastWeek ? "last week" : "this week") +
                " (<t:" + timestampStart + ":R> - <t:" + timestampEnd + ":R>)";

        return new EmbedBuilder()
                .setAuthor(staff.getEffectiveName(), null, staff.getEffectiveAvatarUrl())
                .setTitle("Ticket Count for " + staff.getName())
                .setDescription(timeWindow)
                .addField("Ticket Count", String.valueOf(ticketCount), true)
                .setFooter("number of bans, without duplicates and without those who were unbanned.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.USER, "staff", "The staff member to count tickets for. (defaults to you)", false)
                .addOption(OptionType.BOOLEAN, "lastweek", "Should the data be last week's data? (defaults to false)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        if (StaffRoles.blockIfNotStaff(e))
            return;

        OptionMapping staffOption = e.getOption("staff");
        OptionMapping lastWeekOption = e.getOption("lastweek");

        User staff = staffOption == null ? e.getUser() : staffOption.getAsUser();
        Long staffRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(staff.getIdLong());

        if (staffRobloxId == null) {
            e.reply("Couldn't get " + staff.getName() + "'s Roblox ID from their Discord ID. Are they a staff member with a linked account?").setEphemeral(true).queue();
            return;
        }

        boolean lastWeek = lastWeekOption != null && lastWeekOption.getAsBoolean();

        long currentTimeMillis = System.currentTimeMillis();
        long weekEnd, weekStart;

        if (lastWeek) {
            weekEnd = calculateWeekStart(currentTimeMillis);
            weekStart = calculateWeekStart(currentTimeMillis, 1);
        } else {
            weekEnd = currentTimeMillis;
            weekStart = calculateWeekStart(currentTimeMillis);
        }

        // count all bans between the two dates
        int ticketCount = AWBans.countBansByStaff(staffRobloxId, weekStart, weekEnd);
        e.replyEmbeds(buildTicketCountEmbed(staff, ticketCount, lastWeek, weekStart, weekEnd).build()).queue();
    }
}
