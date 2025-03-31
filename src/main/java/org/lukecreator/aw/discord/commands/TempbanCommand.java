package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.AWTicketsManager;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.BanRequest;

import java.sql.SQLException;

public class TempbanCommand extends BotCommand {
    public TempbanCommand() {
        super("aw-tempban", "(staff only) Ban a user temporarily from Ability Wars.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to tempban.", true)
                .addOption(OptionType.INTEGER, "days", "The duration of the ban in days.", true)
                .addOption(OptionType.STRING, "reason", "The reason for the ban.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        var daysOption = e.getOption("days");
        var reasonOption = e.getOption("reason");

        if (usernameOption == null || daysOption == null || reasonOption == null)
            return;

        String targetUsername = usernameOption.getAsString();
        String reason = reasonOption.getAsString();

        int days = daysOption.getAsInt();
        long durationMs = days * 24 * 60 * 60 * 1000L;

        if (StaffRoles.blockIfNotStaff(e))
            return;

        RobloxAPI.User targetUser = RobloxAPI.getUserByInput(targetUsername);
        Long responsibleModerator = DiscordRobloxLinks.robloxIdFromDiscordId(e.getUser().getIdLong());

        if (targetUser == null) {
            e.reply(getUnknownUsernameDescriptor(targetUsername)).queue();
            return;
        }
        if (responsibleModerator == null) {
            e.reply("Couldn't get your Roblox ID from your Discord ID. If you haven't already, get your accounts linked with an administrator.").setEphemeral(false).queue();
            return;
        }

        AWTicket ticket = null;
        if (e.getChannelType() == ChannelType.TEXT) {
            TextChannel channel = e.getChannel().asTextChannel();
            ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(channel);
        }

        Long ticketId = ticket == null ? null : ticket.id;
        Long evidenceId = ticket == null ? null : (ticket instanceof AWPlayerReportTicket reportTicket) ?
                reportTicket.getEvidenceId() : null;

        e.deferReply().queue();
        PendingRequest request = new BanRequest(PendingRequest.getNextRequestId(), targetUser.userId(), responsibleModerator, reason, false, durationMs, evidenceId, ticketId)
                .onFulfilled(ignored -> e.getInteraction().getHook().editOriginal("Successfully banned user [" + targetUser.username() + "](" + targetUser.getProfileURL() + ") for " + days + " days.").queue())
                .onNoPermission(() -> e.getInteraction().getHook().editOriginal("You don't have permission to temp-ban users in-game.").queue());
        PendingRequests.add(request);
    }
}
