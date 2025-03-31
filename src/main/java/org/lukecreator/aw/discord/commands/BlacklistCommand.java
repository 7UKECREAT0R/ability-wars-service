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

public class BlacklistCommand extends BotCommand {
    public BlacklistCommand() {
        super("appeal-blacklist", "(staff only) Blacklist a Discord/Roblox user from appealing.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addSubcommands(
                        new SubcommandData("discord", "Blacklist a Discord user from appealing.")
                                .addOption(OptionType.USER, "user", "The user to blacklist.", true)
                                .addOption(OptionType.STRING, "reason", "The reason for the blacklist.", false),
                        new SubcommandData("roblox", "Blacklist a Roblox user from appealing.")
                                .addOption(OptionType.STRING, "user", "The Roblox username or ID to blacklist.", true)
                                .addOption(OptionType.STRING, "reason", "The reason for the blacklist.", false)
                );
    }

    public void executeDiscord(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");
        OptionMapping reasonMapping = e.getOption("reason");

        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        User issuer = e.getUser();
        User target = userMapping.getAsUser();
        String reason = reasonMapping == null ? null : reasonMapping.getAsString();
        if (reason != null && reason.isBlank())
            reason = null;

        // adds to database
        DiscordAppealBlacklist.push(target.getIdLong(), issuer.getIdLong(), reason, System.currentTimeMillis());

        AWUnbanTicket.sendTranscriptsMessageRaw(e.getJDA(), "%s has blacklisted discord user %s (id: `%d`). Reason: `%s`"
                .formatted(issuer.getAsMention(), target.getAsMention(), target.getIdLong(), reason == null ? "No reason specified." : reason.replace("`", ""))).queue();
        e.reply("Successfully blacklisted %s from appealing their Discord ban.".formatted(target.getAsMention()))
                .setEphemeral(true).queue();
    }

    public void executeRoblox(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");
        OptionMapping reasonMapping = e.getOption("reason");

        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        User issuer = e.getUser();
        String targetInput = userMapping.getAsString();
        String reason = reasonMapping == null ? null : reasonMapping.getAsString();

        RobloxAPI.User target = RobloxAPI.getUserByInput(targetInput);
        if (target == null) {
            String descriptor = BotCommand.getUnknownUsernameDescriptor(targetInput);
            e.reply(descriptor).setEphemeral(true).queue();
            return;
        }

        AWPlayer targetPlayer = AWPlayer.loadFromDatabase(target.userId(), false, false, false, false);
        targetPlayer.setBlacklist(reason, issuer);

        AWUnbanTicket.sendTranscriptsMessageRaw(e.getJDA(), "%s has blacklisted the player %s [Roblox Profile](%s). Reason: `%s`"
                .formatted(issuer.getAsMention(), target.username(), target.getProfileURL(), reason == null ? "No reason specified." : reason.replace("`", ""))).queue();
        e.reply("Successfully blacklisted [%s](%s) from appealing their Roblox ban.".formatted(target.username(), target.getProfileURL()))
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
