package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWBans;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.stream.Collectors;

public class TicketCountAllCommand extends BotCommand {
    public TicketCountAllCommand() {
        super("ticketcount-all", "Count the number of tickets the whole staff team did this week. (or last week)");
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

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.BOOLEAN, "lastweek", "Should the data be last week's data? (defaults to false)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        if (StaffRoles.blockIfNotStaff(e))
            return;

        Guild aw = e.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);

        if (aw == null) {
            e.reply("The Ability Wars server isn't initialized right now. Try again in a little bit or complain to luke if it continues not working.").setEphemeral(true).queue();
            return;
        }

        OptionMapping lastWeekOption = e.getOption("lastweek");
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

        // collect all staff members and count their tickets.
        e.reply("Counting tickets... This may take a little while.").setEphemeral(false).queue();
        HashMap<Long, Long> discordRobloxLinks = DiscordRobloxLinks.getAllLinks();

        aw.retrieveMembers(
                discordRobloxLinks.keySet()
                        .stream()
                        .map(UserSnowflake::fromId)
                        .collect(Collectors.toList())
        ).onSuccess(staffMembers -> {
            long startedMs = System.currentTimeMillis();
            String timeWindow = "Time period: " + (lastWeek ? "last week" : "this week") +
                    " (<t:" + (weekStart / 1000) + ":R> - <t:" + (weekEnd / 1000) + ":R>)";
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Ticket Counts for All Staff")
                    .setDescription(timeWindow)
                    .setFooter("number of bans, without duplicates and without those who were unbanned.");
            eb.appendDescription("\n");

            for (Member staffMember : staffMembers) {
                long discordId = staffMember.getIdLong();
                if (!StaffRoles.hasStaffRole(staffMember))
                    continue;

                long robloxId = discordRobloxLinks.get(discordId);
                RobloxAPI.User roblox = RobloxAPI.getUserById(robloxId);

                if (roblox == null)
                    continue;

                // count the number of tickets this staff has done
                try {
                    int ticketCount = AWBans.countBansByStaff(robloxId, weekStart, weekEnd);
                    eb.appendDescription("\n- %s %d".formatted(roblox.username(), ticketCount));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.getHook().editOriginalEmbeds(eb.build()).setContent("Completed after %dms".formatted(System.currentTimeMillis() - startedMs)).queue();
        }).onError(throwable -> {
            e.getHook().editOriginal("Failed to retrieve staff members. Error in question:\n```\n" + throwable + "\n```").queue();
        });
    }
}
