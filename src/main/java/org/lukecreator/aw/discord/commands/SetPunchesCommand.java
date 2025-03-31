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
import org.lukecreator.aw.webserver.fulfillments.SetPunchesFulfillment;
import org.lukecreator.aw.webserver.requests.SetPunchesRequest;

import java.sql.SQLException;

public class SetPunchesCommand extends BotCommand {
    public SetPunchesCommand() {
        super("aw-setpunches", "(staff only) Sets a player's punches in game.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to set the punches of.", true)
                .addOption(OptionType.INTEGER, "punches", "The number of punches to set.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        var punchesOption = e.getOption("punches");

        if (usernameOption == null || punchesOption == null)
            return;

        String targetUsername = usernameOption.getAsString();
        int punches = punchesOption.getAsInt();

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

        e.deferReply().queue();

        PendingRequest request = new SetPunchesRequest(PendingRequest.getNextRequestId(), targetUser.userId(), responsibleModerator, punches)
                .onFulfilled(_fulfillment -> {
                    SetPunchesFulfillment fulfillment = (SetPunchesFulfillment) _fulfillment;
                    long oldPunches = fulfillment.oldPunches;
                    long newPunches = fulfillment.newPunches;
                    e.getInteraction().getHook().editOriginal("Changed [" + targetUser.username() + "](" + targetUser.getProfileURL() + ")'s punches from " + oldPunches + " to " + newPunches + " successfully.").queue();
                })
                .onNoPermission(() -> e.getInteraction().getHook().editOriginal("You don't have permission to set punches in-game.").queue());
        PendingRequests.add(request);
    }
}
