package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction;
import org.jetbrains.annotations.NotNull;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.AWTicketsManager;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.data.tickets.AWUnbanTicket;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class TicketManageCommand extends BotCommand {
    private static final long DISCORD_PUNISHMENTS_CHANNEL = 1329630636499144764L;

    public TicketManageCommand() {
        super("ticket", "Manage tickets.");
    }

    private static void sendToDiscordPunishmentsChannel(Guild guild, String message) {
        TextChannel channel = guild.getTextChannelById(DISCORD_PUNISHMENTS_CHANNEL);
        if (channel == null)
            return;
        channel.sendMessage(message).queue();
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addSubcommands(
                        new SubcommandData("cleanup", "Run a cleaning cycle for any dead/broken tickets."),
                        new SubcommandData("close", "Close the ticket.")
                                .addOption(OptionType.STRING, "reason", "The reason for closing the ticket. Not required.", false),
                        new SubcommandData("blacklist", "Blacklist the creator of this ticket from opening future tickets.")
                                .addOption(OptionType.STRING, "reason", "The reason for blacklisting the ticket creator.", false),
                        new SubcommandData("top", "Jump to the top of the ticket to get back to the panel."),
                        new SubcommandData("modify", "Modify a property of this ticket. Depends on the type of ticket.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "ticket-property", "The property to change.", true, true),
                                        new OptionData(OptionType.STRING, "value", "The value to set the property to.", false, false)
                                ),
                        new SubcommandData("nickname", "Give the ticket's channel a nickname.")
                                .addOption(OptionType.STRING, "name", "The nickname to give the ticket channel. Don't include the type prefix.", true),
                        new SubcommandData("history", "Get ticket history for a particular Discord user.")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "The Discord user to get ticket history for.", true),
                                        new OptionData(OptionType.STRING, "type-filter", "The type of ticket to filter by, if specified.", false)
                                                .addChoices(AWTicket.Type.AS_COMMAND_CHOICES)
                                ),
                        new SubcommandData("recall", "Recall a past ticket by its ID.")
                                .addOption(OptionType.INTEGER, "ticket-id", "The ID of the ticket to recall.", true)
                );
    }

    private void executeCleanup(SlashCommandInteractionEvent e) {
        e.deferReply(false).queue();
        final StringBuffer sb = new StringBuffer();

        AWTicketsManager.deadTicketCleanup(e.getJDA(),
                ticketsToCleanup -> {
                    sb.append("Cleaning up ").append(ticketsToCleanup).append(" tickets...");
                    e.getHook().editOriginal(sb.toString()).queue();
                },
                errors -> {
                    sb.append('\n');
                    if (errors.isEmpty())
                        sb.append("Successfully cleaned up all tickets without any errors.");
                    else {
                        sb.append("Success, but encountered ").append(errors.size()).append(" error(s) while cleaning up tickets:");
                        for (String error : errors)
                            sb.append("\n- `").append(error).append('`');
                    }
                    e.getHook().editOriginal(sb.toString()).queue();
                }
        );
    }

    private void executeHistory(SlashCommandInteractionEvent e) {
        OptionMapping userMapping = e.getOption("user");
        OptionMapping typeFilterMapping = e.getOption("type-filter");
        if (userMapping == null) {
            e.reply("You must specify the Discord user to get ticket history for.").setEphemeral(true).queue();
            return;
        }

        User user = userMapping.getAsUser();
        AWTicket.Type typeFilter = null;
        if (typeFilterMapping != null) {
            String typeFilterString = typeFilterMapping.getAsString();
            if (!typeFilterString.isBlank()) {
                typeFilter = AWTicket.Type.fromIdentifier(typeFilterString);
                if (typeFilter == null) {
                    e.reply("Couldn't find a ticket type with the identifier `%s`.".formatted(typeFilterString)).setEphemeral(true).queue();
                    return;
                }
            }
        }

        try {
            AWTicket[] ticketHistory = AWTicket.loadByOwner(user);
            int totalTickets;
            if (typeFilter != null) {
                totalTickets = 0;
                for (AWTicket ticket : ticketHistory)
                    if (ticket.type() == typeFilter)
                        totalTickets++;
            } else {
                totalTickets = ticketHistory.length;
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                    .setTitle("Ticket History")
                    .setColor(Color.DARK_GRAY)
                    .setDescription("Found %d tickets by the user %s.".formatted(totalTickets, user.getAsMention()));
            if (typeFilter != null)
                eb.appendDescription("\n- Showing only tickets of type **%s**".formatted(typeFilter.title));

            int currentTicketsInEmbed = 0;
            for (AWTicket ticket : ticketHistory) {
                if (typeFilter != null && ticket.type() != typeFilter)
                    continue;
                StringBuilder ticketDescription = new StringBuilder();
                ticketDescription.append("- Opened on <t:").append(ticket.openedTimestamp / 1000L).append(":f>\n");
                if (ticket.isOpen()) {
                    ticketDescription.append("- Currently open: <#").append(ticket.getDiscordChannelId()).append('>');
                } else {
                    ticketDescription.append("- Closed by <@").append(ticket.closedByDiscordId).append('>');
                    if (ticket.closeReason != null && !ticket.closeReason.isBlank()) {
                        ticketDescription.append(" For reason:\n-# ").append(ticket.closeReason.replace("\n", "\n-# "));
                    }
                }
                eb.addField(ticket.type().channelPrefix + ticket.id, ticketDescription.toString(), false);
                currentTicketsInEmbed++;
                if (currentTicketsInEmbed >= MessageEmbed.MAX_FIELD_AMOUNT) {
                    eb.setFooter("Truncated to the 25 latest entries due to Discord limitation.");
                    break;
                }
            }
            e.replyEmbeds(eb.build()).setEphemeral(true).queue();
        } catch (SQLException ex) {
            e.reply("A database error occurred while retrieving the ticket history:\n```\n" + ex + "\n```").setEphemeral(true).queue();
        }
    }

    private void executeRecall(SlashCommandInteractionEvent e) {
        OptionMapping ticketIdMapping = e.getOption("ticket-id");
        if (ticketIdMapping == null) {
            e.reply("You must specify the ticket ID to recall.").setEphemeral(true).queue();
            return;
        }

        long ticketId = ticketIdMapping.getAsLong();

        try {
            AWTicket recalled = AWTicket.loadFromDatabase(ticketId);
            if (recalled == null) {
                e.reply("Couldn't find any past tickets with that ID.").setEphemeral(true).queue();
                return;
            }

            recalled.afterCacheLoaded();
            List<MessageEmbed> embeds = recalled.getInitialMessageEmbeds(e.getJDA());
            e.replyEmbeds(embeds)
                    .setContent("Recalled ticket `#%d`, opened by Discord user <@%d>".formatted(ticketId, recalled.ownerDiscordId))
                    .setEphemeral(true)
                    .queue();

        } catch (SQLException ex) {
            e.reply("A database error occurred while recalling the ticket:\n```\n" + ex + "\n```").setEphemeral(true).queue();
        }

    }

    private void executeClose(@NotNull AWTicket ticket, SlashCommandInteractionEvent e) {
        OptionMapping reasonMapping = e.getOption("reason");
        String reason;

        if (reasonMapping == null || reasonMapping.getAsString().isBlank())
            reason = null;
        else
            reason = reasonMapping.getAsString();

        User closer = e.getUser();
        e.reply("Closing...").queue();

        try {
            ticket.close(e.getJDA(), closer, reason, null);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    private void executeBlacklist(Guild guild, @NotNull AWTicket ticket, SlashCommandInteractionEvent e) {
        OptionMapping reasonMapping = e.getOption("reason");
        String reason;

        if (reasonMapping == null || reasonMapping.getAsString().isBlank())
            reason = null;
        else
            reason = reasonMapping.getAsString();

        User issuer = e.getUser();
        e.deferReply(true).queue();

        if (ticket instanceof AWUnbanTicket unbanTicket) {
            try {
                unbanTicket.blacklist(issuer, reason);
                e.getHook().editOriginal("User has been successfully blacklisted from appealing.").queue();
            } catch (SQLException ex) {
                e.getHook().editOriginal("A database error occurred while blacklisting the user:\n```\n" + ex + "\n```").queue();
                return;
            }
        } else if (ticket instanceof AWPlayerReportTicket playerReportTicket) {
            if (guild.getIdLong() != AbilityWarsBot.AW_GUILD_ID) {
                e.getHook().editOriginal("How did you even do this?").queue();
                return;
            }
            long ownerDiscordId = playerReportTicket.ownerDiscordId;
            guild.retrieveMemberById(ownerDiscordId).queue(ownerMember -> {
                Role blacklistRole = guild.getRoleById(AWPlayerReportTicket.BLACKLIST_ROLE);
                if (blacklistRole == null) {
                    e.getHook().editOriginal("The blacklist role is missing, I can't blacklist this user.").queue();
                    return;
                }
                guild.addRoleToMember(ownerMember, blacklistRole).queue(success -> {
                    if (reason != null)
                        sendToDiscordPunishmentsChannel(guild, "%d blacklisted from player reports. %s".formatted(ownerDiscordId, reason));
                    else
                        sendToDiscordPunishmentsChannel(guild, "%d blacklisted from player reports.".formatted(ownerDiscordId));

                    e.getHook().editOriginal("Successfully blacklisted the user and submitted a report in <#%d>.".formatted(AWPlayerReportTicket.BLACKLIST_ROLE)).queue();
                }, failure -> {
                    e.getHook().editOriginal("Failed to give the user the blacklisted role. Error from discord: %s".formatted(failure.getMessage())).queue();
                    return;
                });
            }, failure -> {
                e.getHook().editOriginal("Failed to retrieve the member that opened this ticket. Are they still in the server?").queue();
                return;
            });
        } else {
            e.getHook().editOriginal("This ticket type (`%s`) doesn't support blacklisting.".formatted(ticket.type().identifier)).queue();
            return;
        }
    }

    private void executeTop(TextChannel tc, SlashCommandInteractionEvent e) {
        e.deferReply(true).queue();

        tc.getIterableHistory()
                .order(PaginationAction.PaginationOrder.FORWARD)
                .takeAsync(1)
                .thenApply(messages -> {
                    Message topMessage = messages.get(0);
                    e.getHook().editOriginal("Found message. [Jump to the top of this ticket.](%s)".formatted(topMessage.getJumpUrl())).queue();
                    return null;
                })
                .exceptionally(error -> {
                    e.getHook().editOriginal("Failed to find the top message. Error: `%s`".formatted(error.getMessage())).queue();
                    return null;
                });
    }

    private void executeModify(@NotNull AWTicket ticket, SlashCommandInteractionEvent e) {
        OptionMapping ticketPropertyMapping = e.getOption("ticket-property");
        OptionMapping valueMapping = e.getOption("value");

        if (ticketPropertyMapping == null) {
            e.reply("You must specify a ticket property.").setEphemeral(true).queue();
            return;
        }

        String ticketProperty = ticketPropertyMapping.getAsString();
        String valueAsString = valueMapping != null ? valueMapping.getAsString() : null;

        if (ticketProperty.isBlank()) {
            e.reply("The ticket property name can't be blank.").setEphemeral(true).queue();
            return;
        }

        try {
            ticket.setProperty(ticketProperty, valueAsString, e);
        } catch (SQLException ex) {
            if (e.isAcknowledged()) {
                e.getHook().editOriginal("A database error occurred while modifying the ticket's property:\n```\n" + ex + "\n```").queue();
            } else {
                e.reply("A database error occurred while modifying the ticket's property:\n```\n" + ex + "\n```").setEphemeral(true).queue();
            }
        }
    }

    private void executeNickname(TextChannel tc, @NotNull AWTicket ticket, SlashCommandInteractionEvent e) {
        OptionMapping nameMapping = e.getOption("name");

        if (nameMapping == null) {
            e.reply("You must specify the nickname for the ticket.").setEphemeral(true).queue();
            return;
        }

        String channelName = nameMapping.getAsString().toLowerCase();

        if (channelName.isBlank()) {
            e.reply("The nickname can't be blank.").setEphemeral(true).queue();
            return;
        }

        String channelPrefix = ticket.type().channelPrefix;
        if (!channelName.startsWith(channelPrefix))
            channelName = channelPrefix + channelName;
        if (channelName.contains(" "))
            channelName = channelName.replace(" ", "-");
        if (channelName.length() > TextChannel.MAX_NAME_LENGTH)
            channelName = channelName.substring(0, TextChannel.MAX_NAME_LENGTH);

        e.deferReply(true).queue();
        tc.getManager().setName(channelName).queue(success -> {
            e.getHook().editOriginal("Nickname applied!").queue();
        }, failure -> {
            e.getHook().editOriginal("Failed to apply the nickname. Reason sent by Discord: " + failure.getMessage()).queue();
        });
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        if (!e.isFromGuild()) {
            e.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        Guild guild = e.getGuild();

        String subcommandName = e.getSubcommandName();
        if (subcommandName == null) {
            e.reply("You must use one of the supported subcommands.").setEphemeral(true).queue();
            return;
        }

        if (StaffRoles.blockIfNotStaff(e))
            return;

        // non-ticket required commands
        if (subcommandName.equals("cleanup")) {
            this.executeCleanup(e);
            return;
        }
        if (subcommandName.equals("history")) {
            this.executeHistory(e);
            return;
        }
        if (subcommandName.equals("recall")) {
            this.executeRecall(e);
            return;
        }

        if (e.getChannelType() != ChannelType.TEXT) {
            e.reply("This command can only be used in a ticket's channel.").setEphemeral(true).queue();
            return;
        }

        // try to find an open ticket that uses this channel
        TextChannel tc = e.getChannel().asTextChannel();
        AWTicket ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(tc);

        if (ticket == null) {
            e.reply("This command can only be used in a ticket's channel.").setEphemeral(true).queue();
            return;
        }

        switch (subcommandName) {
            case "close":
                this.executeClose(ticket, e);
                break;
            case "blacklist":
                this.executeBlacklist(guild, ticket, e);
                break;
            case "top":
                this.executeTop(tc, e);
                break;
            case "modify":
                this.executeModify(ticket, e);
                break;
            case "nickname":
                this.executeNickname(tc, ticket, e);
        }
    }
}
