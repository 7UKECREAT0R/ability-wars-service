package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.data.DiscordAppealBlacklist;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.awt.*;
import java.sql.SQLException;

public class BlacklistInfoCommand extends BotCommand {
    public BlacklistInfoCommand() {
        super("appeal-blacklist-info", "(staff only) View information about a user's appeal blacklist.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addSubcommands(
                        new SubcommandData("discord", "View Discord appeal blacklist info.")
                                .addOption(OptionType.USER, "user", "The user to view the blacklist info for.", true),
                        new SubcommandData("roblox", "View Roblox appeal blacklist info.")
                                .addOption(OptionType.STRING, "user", "The Roblox username or ID to view the blacklist info for.", true)
                );
    }

    public void executeDiscord(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");

        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        User target = userMapping.getAsUser();
        long targetId = target.getIdLong();

        // check if they are even blacklisted in the first place
        DiscordAppealBlacklist blacklist = DiscordAppealBlacklist.get(targetId);

        if (blacklist == null) {
            e.reply("That user isn't currently blacklisted.").setEphemeral(true).queue();
            return;
        }

        Guild abilityWars = e.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);
        if (abilityWars == null) {
            e.reply("The Ability Wars Discord server is not available right now; try again in a couple of minutes.")
                    .setEphemeral(true).queue();
            return;
        }

        // send "bot is thinking..."
        e.deferReply(true).queue();

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Blacklist Info")
                .setColor(Color.RED)
                .addField("User", target.getAsMention(), true)
                .addField("Reason", blacklist.reason == null ? "No reason provided." : blacklist.reason, true)
                .addField("Issued By", UserSnowflake.fromId(blacklist.issuerId).getAsMention(), true)
                .addField("Issued Date", "<t:%s:f>".formatted(blacklist.date / 1000L), true);

        abilityWars.retrieveBan(target).queue(ban -> {
            eb.setDescription("User is banned from the Discord, and unable to appeal their ban.");
            e.getHook().editOriginalEmbeds(eb.build()).queue();
        }, notBanned -> {
            eb.setDescription("User is **not** banned from the Discord currently, but is blacklisted.");
            e.getHook().editOriginalEmbeds(eb.build()).queue();
        });
    }

    public void executeRoblox(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping userMapping = e.getOption("user");
        
        if (userMapping == null) {
            e.reply("User wasn't present?").setEphemeral(true).queue();
            return;
        }

        String targetInput = userMapping.getAsString();

        RobloxAPI.User target = RobloxAPI.getUserByInput(targetInput);
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

        String reason = targetPlayer.getAppealBlacklistReason();
        if (reason == null)
            reason = "No reason provided.";

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Blacklist Info")
                .setColor(Color.RED)
                .addField("User", "%s, [Roblox Profile](%s)".formatted(target.username(), target.getProfileURL()), true)
                .addField("Reason", reason, true)
                .addField("Issued By", UserSnowflake.fromId(targetPlayer.getAppealBlacklistIssuer()).getAsMention(), true)
                .addField("Issued Date", "<t:%s:f>".formatted(targetPlayer.getAppealBlacklistDate() / 1000L), true);
        e.replyEmbeds(eb.build()).setEphemeral(true).queue();
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
