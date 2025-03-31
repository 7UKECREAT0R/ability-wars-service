package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.discord.BotCommand;

import java.sql.SQLException;

public class OpenTicketCommand extends BotCommand {

    public OpenTicketCommand() {
        super("open", "Open a ticket.");
    }

    @Override
    public SlashCommandData constructCommand() {
        SlashCommandData builder = Commands.slash(this.name, this.description);
        AWTicket.Type[] ticketTypes = AWTicket.Type.values();
        SubcommandData[] subcommands = new SubcommandData[ticketTypes.length];

        for (int i = 0; i < ticketTypes.length; i++) {
            AWTicket.Type type = ticketTypes[i];
            String typeId = type.identifier;
            String typeName = type.description;
            subcommands[i] = new SubcommandData(typeId, typeName);
        }

        builder.addSubcommands(subcommands);
        return builder;
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        String typeId = e.getSubcommandName();
        if (typeId == null) {
            e.reply("You must specify a ticket type.").setEphemeral(true).queue();
            return;
        }

        AWTicket.Type ticketType = AWTicket.Type.fromIdentifier(typeId);
        if (ticketType == null) {
            e.reply("Unknown ticket type (this is definitely a bug): \"%s\"".formatted(typeId)).setEphemeral(true).queue();
            return;
        }

        if (ticketType == AWTicket.Type.PlayerReport) {
            Member m = e.getMember();
            if (m != null && m.getRoles().stream().anyMatch(role -> role.getIdLong() == AWPlayerReportTicket.BLACKLIST_ROLE)) {
                e.reply("You're currently blacklisted from creating player reports.\n-# This may be due to failing to follow the requirements too many times, opening tickets as a joke, or other unhelpful infractions.").setEphemeral(false).queue();
                return;
            }
        }

        try {
            Modal modal = AWTicket.tryOpenNewTicket(ticketType, e.getGuild(), e.getUser());
            e.replyModal(modal).queue();
        } catch (SQLException sqlException) {
            e.reply("An internal error occurred while trying to open a ticket. Please try again later.\n```\n%s\n```"
                    .formatted(sqlException.toString())).setEphemeral(true).queue();
            return;
        } catch (IllegalArgumentException otherException) {
            String userFriendlyMessage = otherException.getMessage();
            e.reply(userFriendlyMessage).setEphemeral(true).queue();
        }
    }
}
