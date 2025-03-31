package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.ApprovedAwesomeAdministratorsAdmins;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;

import java.sql.SQLException;
import java.util.Collections;

public class RobloxSearchCommand extends BotCommand {
    public RobloxSearchCommand() {
        super("roblox-search", "Search for a Roblox user by username.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to search for.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        if (usernameOption == null)
            return;
        String username = usernameOption.getAsString();
        RobloxAPI.User user = RobloxAPI.getUserByInput(username);

        if (user == null) {
            e.reply(getUnknownUsernameDescriptor(username)).queue();
            return;
        }

        // link information
        Member callingUser = e.getMember();
        if (callingUser == null) {
            e.reply("Who are you?").setEphemeral(true).queue();
            return;
        }

        Long linkedDiscordId = DiscordRobloxLinks.discordIdFromRobloxId(user.userId());
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Roblox Search")
                .setDescription("Search results for \"" + username + "\"")
                .setColor(callingUser.getColor());

        eb.addField("Username", user.username(), true);
        eb.addField("ID", String.valueOf(user.userId()), true);
        String bio = user.bio();
        if (bio != null && !bio.isBlank())
            eb.addField("Bio", user.bio(), false);

        if (linkedDiscordId != null) {
            // extended information
            boolean isAdmin = ApprovedAwesomeAdministratorsAdmins.isApprovedAwesomeAdministratorAdmin(linkedDiscordId);
            eb.addField("Linked Discord", "<@" + linkedDiscordId + ">", true);
            eb.addField("Bot Admin", isAdmin ? "Yes" : "No", true);
        }

        e.replyEmbeds(eb.build()).setAllowedMentions(Collections.emptyList()).setEphemeral(false).queue();
    }
}
