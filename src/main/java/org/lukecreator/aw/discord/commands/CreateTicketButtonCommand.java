package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.tickets.AWUnbanTicket;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.awt.*;
import java.sql.SQLException;

public class CreateTicketButtonCommand extends BotCommand {
    public CreateTicketButtonCommand() {
        super("create-ticket-button", "Creates a message with a button that allows the user to open a ticket.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOptions(
                        new OptionData(OptionType.STRING, "type", "The type of ticket to create.")
                                .addChoices(AWTicket.Type.AS_COMMAND_CHOICES)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        OptionMapping typeOption = e.getOption("type");
        if (typeOption == null) {
            e.reply("You must specify a ticket type.").setEphemeral(true).queue();
            return;
        }

        String typeId = typeOption.getAsString();
        AWTicket.Type ticketType = AWTicket.Type.fromIdentifier(typeId);

        if (ticketType == null) {
            e.reply("Unknown ticket type: \"%s\"".formatted(typeId)).setEphemeral(true).queue();
            return;
        }

        if (StaffRoles.blockIfNotStaff(e))
            return;

        MessageEmbed embed;
        switch (ticketType) {
            case PlayerReport -> {
                embed = new EmbedBuilder()
                        .setColor(new Color(65, 162, 232))
                        .setTitle("Report a Player")
                        .setDescription("If you've found somebody breaking the rules, open a ticket here so that a staff member can help remove them from the game! We require video evidence generally.")
                        .build();
            }
            case BanAppeal -> {
                embed = new EmbedBuilder()
                        .setColor(new Color(65, 232, 129))
                        .setTitle("Open an Appeal")
                        .setDescription("If you've been fairly banned for 6 months or longer, you may open an appeal to be unbanned. Please read <#" + AWUnbanTicket.INFO_CHANNEL_ID + "> first before doing so!")
                        .build();
            }
            case BanDispute -> {
                embed = new EmbedBuilder()
                        .setColor(new Color(232, 187, 65))
                        .setTitle("Open a Dispute")
                        .setDescription("If you believe you were banned by mistake, open a dispute and we'll do our best to re-review the incident more closely. We also can make mistakes.")
                        .build();
            }
            case BlacklistAppeal -> {
                embed = new EmbedBuilder()
                        .setColor(new Color(114, 50, 161))
                        .setTitle("Open a Blacklist Appeal")
                        .setDescription("If you repeatedly created bad tickets, low-effort suggestions, or other various wastes of our time, you may have been blacklisted. Open a ticket here if you wish to re-gain access to player reports, suggestions, or bug reports.")
                        .build();
            }
            default -> {
                e.reply("Unknown ticket type: \"%s\"".formatted(typeId)).setEphemeral(true).queue();
                return;
            }
        }

        e.getChannel().sendMessageEmbeds(embed)
                .addActionRow(Button.of(
                        ButtonStyle.SUCCESS,
                        "tryopen_" + ticketType.identifier,
                        "Open " + ticketType.title,
                        Emoji.fromUnicode("\uD83C\uDFAB"))
                ).queue();
        e.reply("Message was created in this channel.").setEphemeral(true).queue();
    }
}
