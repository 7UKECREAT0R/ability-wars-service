package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;

import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

public class DiscordBanCheckCommand extends BotCommand {
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public DiscordBanCheckCommand() {
        super("aw-discord-ban-status", "Checks if a user is banned from the Ability Wars Discord.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.USER, "user", "The Discord user to check.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var userOption = e.getOption("user");
        if (userOption == null)
            return;

        User user = userOption.getAsUser();
        Guild abilityWarsGuild = e.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);

        if (abilityWarsGuild == null) {
            e.reply("The Ability Wars Discord server is not available right now; try again in a couple of minutes.")
                    .setEphemeral(true).queue();
            return;
        }

        BiConsumer<Boolean, Guild.Ban> onBanRetrieved = (isBanned, ban) -> {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(isBanned ? Color.RED : Color.GREEN)
                    .setTitle("Ban Check for " + user.getName());
            if (isBanned) {
                String reason = ban.getReason();
                if (reason == null || reason.isBlank()) {
                    reason = "No reason given. This does __NOT__ mean the ban is invalid!";
                }
                eb.setDescription("The user " + user.getAsMention() + " is banned from the Discord. Reason:\n\n-# " + reason);
            } else {
                eb.appendDescription("The user " + user.getAsMention() + " is not banned from the Discord!");
            }

            e.replyEmbeds(eb.build()).setEphemeral(false).queue();
        };

        abilityWarsGuild.retrieveBan(user).queue(ban -> {
            onBanRetrieved.accept(true, ban);
        }, failure -> {
            onBanRetrieved.accept(false, null);
        });
    }
}