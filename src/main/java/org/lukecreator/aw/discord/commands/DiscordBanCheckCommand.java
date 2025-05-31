package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.BloxlinkAPI;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;

import java.awt.*;
import java.sql.SQLException;
import java.util.function.BiConsumer;

public class DiscordBanCheckCommand extends BotCommand {

    public DiscordBanCheckCommand() {
        super("aw-discord-ban-status", "Checks if a user is banned from the Ability Wars Discord.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.USER, "discord-user", "The Discord user to check.", false)
                .addOption(OptionType.STRING, "roblox-user", "The Roblox username or ID to check.", false);
    }

    public void executeDiscord(SlashCommandInteractionEvent e, User user) {
        Guild abilityWarsGuild = e.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);

        if (abilityWarsGuild == null) {
            e.reply("The Ability Wars Discord server is not available right now; try again in a couple of minutes.")
                    .setEphemeral(true).queue();
            return;
        }

        e.deferReply(true).queue();

        BiConsumer<Boolean, Guild.Ban> onBanRetrieved = (isBanned, ban) -> {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(isBanned ? Color.RED : Color.GREEN)
                    .setTitle("Ban Check for " + user.getName());
            if (isBanned) {
                String reason = ban.getReason();
                if (reason == null || reason.isBlank()) {
                    reason = "No reason specified. This does __NOT__ mean the ban is invalid!";
                }
                eb.setDescription("The user " + user.getAsMention() + " is banned from the Discord. Reason:\n\n-# " + reason);
            } else {
                eb.appendDescription("The user " + user.getAsMention() + " is not banned from the Discord.");
            }

            e.getInteraction().getHook().editOriginalEmbeds(eb.build()).queue();
        };

        abilityWarsGuild.retrieveBan(user).queue(
                ban -> onBanRetrieved.accept(true, ban),
                failure -> onBanRetrieved.accept(false, null)
        );
    }

    public void executeRoblox(SlashCommandInteractionEvent e, String input) throws SQLException {
        Guild abilityWarsGuild = e.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);

        if (abilityWarsGuild == null) {
            e.reply("The Ability Wars Discord server is not available right now; try again in a couple of minutes.")
                    .setEphemeral(true).queue();
            return;
        }

        e.deferReply(true).queue();

        RobloxAPI.User user = RobloxAPI.getUserByInput(input, true);

        if (user == null) {
            e.getInteraction().getHook().editOriginal("Couldn't find any users on Roblox from your input `" + input.replace("`", "") + "`.").queue();
            return;
        }

        long userId = user.userId();
        Long discordId = BloxlinkAPI.lookupDiscordId(userId);

        if (discordId == null) {
            e.getInteraction().getHook().editOriginalEmbeds(new EmbedBuilder()
                    .setColor(Color.red)
                    .setTitle("User not found")
                    .setDescription("The user `" + user.username() + "` was either not in the Discord or is banned; we don't have a way of knowing.")
                    .build()
            ).queue();
            return;
        }

        abilityWarsGuild.retrieveBan(UserSnowflake.fromId(discordId)).queue(ban -> {
            String reason = ban.getReason();
            if (reason == null || reason.isBlank()) {
                reason = "No reason specified. This does __NOT__ mean the ban is invalid!";
            }
            e.getInteraction().getHook().editOriginalEmbeds(new EmbedBuilder()
                    .setColor(Color.red)
                    .setTitle("User banned")
                    .setDescription("The user `" + user.username() + "` is banned from the Discord. Reason:\n\n-# " + reason)
                    .build()
            ).queue();
        }, failure -> e.getInteraction().getHook().editOriginalEmbeds(new EmbedBuilder()
                .setColor(Color.green)
                .setTitle("User not banned")
                .setDescription("The user `" + user.username() + "` is not banned from the Discord.")
                .build()
        ).queue());
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var discordUserOption = e.getOption("discord-user");
        var robloxUserOption = e.getOption("roblox-user");

        if (discordUserOption == null && robloxUserOption == null) {
            e.reply("You must specify either a Discord user or Roblox user to check.").setEphemeral(true).queue();
            return;
        }
        if (discordUserOption != null && robloxUserOption != null) {
            e.reply("You must specify either a Discord user or Roblox user to check, not both.").setEphemeral(true).queue();
            return;
        }

        if (discordUserOption != null) {
            // discord user
            User discordUser = discordUserOption.getAsUser();
            this.executeDiscord(e, discordUser);
            return;
        }

        // roblox user
        String input = robloxUserOption.getAsString();
        this.executeRoblox(e, input);
        return;
    }
}