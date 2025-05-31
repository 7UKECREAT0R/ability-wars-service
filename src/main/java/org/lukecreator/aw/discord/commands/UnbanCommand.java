package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.UnbanRequest;

import java.sql.SQLException;

public class UnbanCommand extends BotCommand {
    public UnbanCommand() {
        super("aw-unban", "(staff only) Unban a user from Ability Wars.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to unban.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        if (usernameOption == null)
            return;

        String targetUsername = usernameOption.getAsString();

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

        e.deferReply().queue();
        PendingRequest request = new UnbanRequest(PendingRequest.getNextRequestId(), targetUser.userId(), responsibleModerator)
                .onFulfilled(ignored -> e.getInteraction().getHook().editOriginal("Successfully unbanned user [" + targetUser.username() + "](" + targetUser.getProfileURL() + ").").queue())
                .onNoPermission(() -> e.getInteraction().getHook().editOriginal("You don't have permission to unban users in-game.").queue());
        PendingRequests.add(request);
    }
}
