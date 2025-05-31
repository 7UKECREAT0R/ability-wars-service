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

public class BanCommand extends BotCommand {
    public BanCommand() {
        super("aw-ban", "(staff only) Ban a user permanently from Ability Wars.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to ban.", true)
                .addOption(OptionType.STRING, "reason", "The reason for the ban.", true);

    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        var reasonOption = e.getOption("reason");
        if (usernameOption == null || reasonOption == null)
            return;

        String targetUsername = usernameOption.getAsString();
        String reason = reasonOption.getAsString();

        if (StaffRoles.blockIfNotStaff(e))
            return;

        RobloxAPI.User targetUser = RobloxAPI.getUserByInput(targetUsername, true);
        Long responsibleModerator = DiscordRobloxLinks.robloxIdFromDiscordId(e.getUser().getIdLong());

        if (targetUser == null) {
            e.reply(getUnknownUsernameDescriptor(targetUsername)).queue();
            return;
        }
        if (responsibleModerator == null) {
            e.reply("Couldn't get your Roblox ID from your Discord ID. If you haven't already, get your accounts linked with an administrator.").setEphemeral(false).queue();
            return;
        }

        AWPlayerReportTicket ticket = null;
        if (e.getChannelType() == ChannelType.TEXT) {
            TextChannel channel = e.getChannel().asTextChannel();
            AWTicket _ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(channel);
            if (_ticket instanceof AWPlayerReportTicket t)
                ticket = t;
        }

        final Long ticketId = ticket == null ? null : ticket.id;
        final Long evidenceId = ticket == null ? null : ticket.getEvidenceId();

        e.deferReply().queue();
        PendingRequest request = new BanRequest(PendingRequest.getNextRequestId(), targetUser.userId(), responsibleModerator, reason, true, 0L, evidenceId, ticketId)
                .onFulfilled(ignored -> {
                    String successMessage = evidenceId != null ?
                            "Successfully banned user [%s](%s) and filed report in <#%d>".formatted(targetUser.username(), targetUser.getProfileURL(), AWPlayerReportTicket.IN_GAME_PUNISHMENTS_CHANNEL) :
                            "Successfully banned user [%s](%s).".formatted(targetUser.username(), targetUser.getProfileURL());
                    e.getInteraction().getHook().editOriginal(successMessage).queue();
                })
                .onNoPermission(() -> e.getInteraction().getHook().editOriginal("You don't have permission to ban users in-game.").queue());
        PendingRequests.add(request);
    }
}
