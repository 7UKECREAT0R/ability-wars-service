package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.ApprovedAwesomeAdministratorsAdmins;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;

import java.awt.*;
import java.sql.SQLException;

public class RobloxLinkCommand extends BotCommand {
    public RobloxLinkCommand() {
        super("staff-roblox-link", "(admins only) Create a link between a Roblox account and Discord account.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, "discord", "The Discord user to link.", true)
                .addOption(OptionType.STRING, "roblox", "The Roblox username to link.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        if (e.getChannelType() != ChannelType.TEXT)
            e.reply("This command can only be used in a server channel.").setEphemeral(true).queue();

        Member member = e.getMember();
        if (member == null) {
            e.reply("Who are you?").setEphemeral(true).queue();
            return;
        }

        if (!ApprovedAwesomeAdministratorsAdmins.isApprovedAwesomeAdministratorAdmin(member.getIdLong())) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Reserved Command");
            eb.setDescription("This command is reserved for admins so that they can link moderator's Discord accounts to their Roblox accounts.");
            eb.setColor(Color.RED);
            eb.setFooter("Use Bloxlink's features instead!");
            e.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        var targetDiscordOption = e.getOption("discord");
        var targetRobloxOption = e.getOption("roblox");

        if (targetDiscordOption == null || targetRobloxOption == null)
            return;

        Member targetDiscordMember = targetDiscordOption.getAsMember();
        String targetRobloxUsername = targetRobloxOption.getAsString();

        if (targetDiscordMember == null) {
            e.reply("Mentioned Discord user doesn't seem to exist.").setEphemeral(true).queue();
            return;
        }
        long targetDiscordId = targetDiscordMember.getIdLong();

        RobloxAPI.User targetRoblox = RobloxAPI.getUserByCurrentUsername(targetRobloxUsername);

        if (targetRoblox == null) {
            e.reply("Couldn't find Roblox user \"" + targetRobloxUsername + "\".").queue();
            return;
        }

        long targetRobloxId = targetRoblox.userId();

        try {
            DiscordRobloxLinks.createLink(targetDiscordId, targetRobloxId);
            e.reply("Created link between " + targetRobloxUsername + " and " + targetDiscordMember.getAsMention()).queue();
        } catch (SQLException ex) {
            e.reply("SQL Exception while trying to create link: " + ex.getMessage()).setEphemeral(true).queue();
        }
    }
}
