package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.data.DiscordAppealBlacklist;
import org.lukecreator.aw.data.tickets.AWUnbanTicket;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.sql.SQLException;

public class BlacklistRemoveCommand extends BotCommand {
    public BlacklistRemoveCommand() {
        super("appeal-blacklist-remove", "(staff only) Allow a Discord/Roblox user to appeal again.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addSubcommands(
                        new SubcommandData("discord", "Allow a Discord user to appeal again.")
                                .addOption(OptionType.USER, "user", "The user to remove the blacklist for.", true),
                        new SubcommandData("roblox", "Allow a Roblox user to appeal again.")
                                .addOption(OptionType.STRING, "user", "The Roblox username or ID to remove the blacklist for.", true)
                );
    }

    public void executeDiscord(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");

        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        User issuer = e.getUser();
        User target = userMapping.getAsUser();
        long targetId = target.getIdLong();

        // check if they are even blacklisted in the first place
        if (!DiscordAppealBlacklist.isBlacklisted(targetId)) {
            e.reply("That user isn't currently blacklisted.").setEphemeral(true).queue();
            return;
        }

        // removes from the database
        DiscordAppealBlacklist.remove(targetId);

        AWUnbanTicket.sendTranscriptsMessageRaw(e.getJDA(), "%s has removed the blacklist from the discord user %s (id: `%d`)."
                .formatted(issuer.getAsMention(), target.getAsMention(), target.getIdLong())).queue();
        e.reply("Successfully removed the Discord ban appeal blacklist from %s.".formatted(target.getAsMention()))
                .setEphemeral(true).queue();
    }

    public void executeRoblox(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");

        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        String targetInput = userMapping.getAsString();

        RobloxAPI.User target = RobloxAPI.getUserByInput(targetInput, true);
        if (target == null) {
            String descriptor = BotCommand.getUnknownUsernameDescriptor(targetInput);
            e.reply(descriptor).setEphemeral(true).queue();
            return;
        }

        AWPlayer targetPlayer = AWPlayer.loadFromDatabase(target.userId(), false, false, false, false);

        if (!targetPlayer.isAppealBlacklisted()) {
            e.reply("That user isn't currently blacklisted.").setEphemeral(true).queue();
            return;
        }

        targetPlayer.removeBlacklist();
        AWUnbanTicket.sendTranscriptsMessageRaw(e.getJDA(), "%s has removed the blacklist from the player %s [Roblox Profile](%s)."
                .formatted(e.getUser().getAsMention(), target.username(), target.getProfileURL())).queue();
        e.reply("Successfully removed the Roblox ban appeal blacklist from [%s](%s).".formatted(target.username(), target.getProfileURL()))
                .setEphemeral(true).queue();
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        if (StaffRoles.blockIfNotStaff(e))
            return;

        String subcommand = e.getSubcommandName();

        if (subcommand == null) {
            e.reply("You must use one of the supported subcommands (discord/roblox).").setEphemeral(true).queue();
            return;
        }

        if (subcommand.equalsIgnoreCase("discord"))
            this.executeDiscord(e);
        else if (subcommand.equalsIgnoreCase("roblox"))
            this.executeRoblox(e);
        else {
            e.reply("You must use one of the supported subcommands (discord/roblox).").setEphemeral(true).queue();
            return;
        }
    }
}
