package org.lukecreator.aw.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jetbrains.annotations.NotNull;
import org.lukecreator.aw.data.AWEvidence;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.AWTicketsManager;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.data.tickets.AWUnbanTicket;
import org.lukecreator.aw.discord.commands.*;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Ability Wars Discord bot.
 */
public class AbilityWarsBot extends ListenerAdapter {
    /**
     * A button ID which will cause the ticket containing the message to be canceled.
     */
    public static final String BUTTON_ID_CANCEL_TICKET_CONFIRM = "cancelticketnow";
    /**
     * A button ID which will cause the message holding the button to be deleted.
     */
    public static final String BUTTON_ID_DELETE_PARENT_MESSAGE = "deletethismessage";
    /**
     * A button ID which will display an embed to the clicker explaining what an IP ban is.
     */
    public static final String BUTTON_ID_EXPLAIN_IP_BAN = "explainipbans";
    /**
     * A button ID which will unregister the attached evidence to it.
     */
    public static final String BUTTON_ID_UNREGISTER_EVIDENCE = "deleteevidence";

    public static final long AW_GUILD_ID = 922921165373202463L;
    public static final long AW_APPEALS_GUILD_ID = 978530758824194088L;
    public static final BotCommand[] ALL_COMMANDS = new BotCommand[]{
            new RobloxLinkCommand(),
            new RobloxSearchCommand(),
            new StatsCommand(),
            new BanCheckCommand(),
            new BanCommand(),
            new MassbanCommand(),
            new TempbanCommand(),
            new UnbanCommand(),
            new SetPunchesCommand(),
            new DiscordBanCheckCommand(),
            new OpenTicketCommand(),
            new TicketCountCommand(),
            new TicketCountAllCommand(),
            new BlacklistCommand(),
            new BlacklistRemoveCommand(),
            new BlacklistInfoCommand(),
            new CreateTicketButtonCommand(),
            new TicketManageCommand()
    };
    private static final String TOKEN = System.getenv("AW_BOT_TOKEN");
    public final JDA bot;

    /**
     * Creates and starts a new Ability Wars bot instance.
     */
    public AbilityWarsBot(boolean registerCommands) {
        JDABuilder builder = JDABuilder
                .createDefault(TOKEN)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.of(Activity.ActivityType.PLAYING, "Skyrift"))
                .setAutoReconnect(true)
                .addEventListeners(this);
        this.bot = builder.build();

        if (registerCommands) this.registerCommands();
    }

    private void registerCommands() {
        CommandListUpdateAction action = this.bot.updateCommands();
        ArrayList<CommandData> data = Arrays.stream(ALL_COMMANDS)
                .map(BotCommand::constructCommand)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        data.add(Commands.message("Add as Evidence"));
        data.add(Commands.message("Set as Main Evidence"));

        System.out.println("Registering " + data.size() + " commands...");
        action.addCommands(data).queue();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Ability Warden is ready to roll!");
        try {
            AWTicketsManager.loadTicketsFromDatabase();
        } catch (SQLException e) {
            System.out.println("Never mind, failed to load tickets from database: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;

        Guild guild = event.getGuild();

        // if an open ticket was tied to that channel, 
        if (event.isFromType(ChannelType.TEXT)) {
            TextChannel tc = event.getChannel().asTextChannel();
            AWTicket ticketTest = AWTicketsManager.getTicketFromCacheByDiscordChannel(tc);
            if (ticketTest != null) {
                // an open ticket's channel was deleted.
                Consumer<User> acceptDeletedUser = (user) -> {
                    boolean hasUser = user != null;
                    JDA jda = hasUser ? user.getJDA() : event.getJDA();
                    try {
                        ticketTest.close(jda, hasUser ? user : jda.getSelfUser(), "No reason specified. (channel was deleted)", null, null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                };

                // try to get who did it from the Audit Log
                guild.retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).limit(1).queue(found -> {
                    if (!found.isEmpty()) {
                        AuditLogEntry entry = found.get(0);
                        if (entry.getTargetIdLong() == tc.getIdLong()) {
                            User user = entry.getUser();
                            acceptDeletedUser.accept(user);
                            return;
                        }
                    }
                    acceptDeletedUser.accept(null);
                }, failure -> acceptDeletedUser.accept(null));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getChannelType() != ChannelType.TEXT) return;
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        // it's guaranteed we're only in a TextChannel in a Guild now

        TextChannel tc = event.getChannel().asTextChannel();

        // forward the message event to its attached ticket, if any
        AWTicket ticketTest = AWTicketsManager.getTicketFromCacheByDiscordChannel(tc);
        if (ticketTest != null) {
            // signal to the ticket about the message
            ticketTest.onMessageReceived(event);
        }
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        String contextCommandName = event.getName().toUpperCase();

        // for now, the only context commands we have are for use in tickets only by staff.
        // if this changes, this method can be refactored then.
        if (StaffRoles.blockIfNotStaff(event))
            return;

        Message message = event.getTarget();
        MessageChannelUnion _channel = message.getChannel();
        if (_channel.getType() != ChannelType.TEXT) {
            event.reply("This action can only be used in a text channel.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = _channel.asTextChannel();
        AWTicket ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(channel);
        if (ticket == null) {
            event.reply("This action can only be used in a ticket.").setEphemeral(true).queue();
            return;
        }

        switch (contextCommandName) {
            case "ADD AS EVIDENCE": {
                if (ticket.type() != AWTicket.Type.PlayerReport) {
                    event.reply("This action can only be used in a player report ticket.").setEphemeral(true).queue();
                    return;
                }
                AWPlayerReportTicket playerReportTicket = (AWPlayerReportTicket) ticket;
                try {
                    String report = playerReportTicket.addExtraEvidenceFromMessage(message);
                    event.replyEmbeds(new EmbedBuilder().setDescription(report).build()).queue();
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.reply("An internal error occurred while trying to add evidence. Please try again later.\n```\n%s\n```".formatted(e.toString())).setEphemeral(true).queue();
                    return;
                }
                return;
            }
            case "SET AS MAIN EVIDENCE": {
                if (ticket.type() != AWTicket.Type.PlayerReport) {
                    event.reply("This action can only be used in a player report ticket.").setEphemeral(true).queue();
                    return;
                }
                AWPlayerReportTicket playerReportTicket = (AWPlayerReportTicket) ticket;
                try {
                    String report = playerReportTicket.changeMainEvidenceFromMessage(message);
                    event.replyEmbeds(new EmbedBuilder().setDescription(report).build()).queue();
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.reply("An internal error occurred while trying to set the main evidence. Please try again later.\n```\n%s\n```".formatted(e.toString())).setEphemeral(true).queue();
                    return;
                }
                return;
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.reply("This bot can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();
        for (BotCommand command : ALL_COMMANDS) {
            if (commandName.equals(command.name)) {
                try {
                    command.execute(event);
                } catch (java.sql.SQLException exception) {
                    if (event.isAcknowledged()) {
                        if (!event.getInteraction().getHook().isExpired()) {
                            event.getInteraction().getHook().editOriginal("The Ability Wars database messed up somehow; try again later.").queue();
                        }
                    } else {
                        event.reply("The Ability Wars database messed up somehow; try again later.").setEphemeral(false).queue();
                    }
                }
                return;
            }
        }

        event.reply("Unimplemented command: " + commandName).setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("This bot can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String id = event.getComponentId();

        if (id.isBlank()) return; // wtf

        String[] chunks = id.split("_");
        String action = chunks[0].toLowerCase(); // the first chunk is the "action" of the modal.

        switch (action) {
            case BUTTON_ID_CANCEL_TICKET_CONFIRM: {
                AWTicket ticket = AWTicketsManager.getTicketFromCacheByDiscordChannelId(event.getChannelIdLong());

                if (ticket == null) {
                    event.reply("This ticket's no longer valid/open. This is not intended.").setEphemeral(true).queue();
                    return;
                }

                Member clickedMember = event.getMember();
                if (clickedMember == null) {
                    event.reply("This button should only appear in a server. This is not intended.").setEphemeral(true).queue();
                    return;
                }

                try {
                    event.deferEdit().queue();
                    ticket.close(event.getJDA(), clickedMember.getUser(), "Cancelled manually", null, null);
                } catch (SQLException sqlException) {
                    event.reply("An internal error occurred while trying to open a ticket. Please try again later.\n```\n%s\n```".formatted(sqlException.toString())).setEphemeral(true).queue();
                }
                break;
            }
            case BUTTON_ID_DELETE_PARENT_MESSAGE: {
                Message message = event.getMessage();
                event.deferEdit().queue();
                message.delete().queue();
                break;
            }
            case BUTTON_ID_EXPLAIN_IP_BAN: {
                MessageEmbed explainerEmbed = AWUnbanTicket.getResponseForIPBan();
                event.replyEmbeds(explainerEmbed).setEphemeral(true).queue();
                break;
            }
            case BUTTON_ID_UNREGISTER_EVIDENCE: {
                if (StaffRoles.blockIfNotStaff(event))
                    return;
                String evidenceIdString = chunks[1];
                long evidenceId = Long.parseLong(evidenceIdString);
                try {
                    AWEvidence.removeFromDatabase(evidenceId);
                    event.deferEdit().queue();
                    event.getMessage()
                            .editMessageComponents()
                            .setContent("Changed the evidence successfully and removed the previous evidence.\n-# Note: the message at the top won't change.")
                            .queue();
                } catch (SQLException e) {
                    event.reply("An internal error occurred while trying to delete the evidence. Please try again later.\n```\n%s\n```".formatted(e.toString())).setEphemeral(true).queue();
                }
            }
            case "ta": {
                String actionId = chunks[1];
                long ticketId = Long.parseLong(chunks[2]);

                AWTicket ticket = AWTicketsManager.getTicketFromCache(ticketId);
                if (ticket == null) {
                    event.reply("This ticket's no longer valid/open. This is not intended.").setEphemeral(true).queue();
                    return;
                }

                ticket.handleAction(actionId, event);
                break;
            }
            case "tryopen": {
                String ticketTypeIdentifier = chunks[1];
                AWTicket.Type ticketType = AWTicket.Type.fromIdentifier(ticketTypeIdentifier);
                if (ticketType == null) {
                    event.reply("Unknown ticket type (this is definitely a bug): \"%s\"".formatted(ticketTypeIdentifier)).setEphemeral(true).queue();
                    return;
                }

                if (ticketType == AWTicket.Type.PlayerReport) {
                    Member m = event.getMember();
                    if (m != null && m.getRoles().stream().anyMatch(role -> role.getIdLong() == AWPlayerReportTicket.BLACKLIST_ROLE)) {
                        event.reply("You're currently blacklisted from creating player reports.\n-# This may be due to failing to follow the requirements too many times, opening tickets as a joke, or other unhelpful infractions.").setEphemeral(true).queue();
                        return;
                    }
                }

                try {
                    Modal modal = AWTicket.tryOpenNewTicket(ticketType, event.getGuild(), event.getUser());
                    event.replyModal(modal).queue();
                } catch (SQLException sqlException) {
                    event.reply("An internal error occurred while trying to open a ticket. Please try again later.\n```\n%s\n```".formatted(sqlException.toString())).setEphemeral(true).queue();
                } catch (IllegalArgumentException otherException) {
                    String userFriendlyMessage = otherException.getMessage();
                    event.reply(userFriendlyMessage).setEphemeral(true).queue();
                }
                break;
            }
            case "evidenceagree": {
                User u = event.getUser();
                event.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Video Evidence Allowed").setAuthor(u.getName(), null, u.getEffectiveAvatarUrl()).setDescription("User agreed to the staff reviewing the video evidence tied to the incident.").setColor(Color.green).build()).queue();
                event.deferEdit().queue();
                event.getMessage().delete().queue(null, failure -> {
                });
                break;
            }
            case "evidencedisagree": {
                User u = event.getUser();
                event.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Video Evidence Disallowed").setAuthor(u.getName(), null, u.getEffectiveAvatarUrl()).setDescription("User does not consent to the staff reviewing the video evidence tied to the incident.").addField("Additional Information", "The user %s will further explain their reason for this decision below:".formatted(u.getAsMention()), false).setColor(Color.red).build()).mention(u).queue();
                event.deferEdit().queue();
                event.getMessage().delete().queue(null, failure -> {
                });
                break;
            }
            case "evidencedelete": {
                if (StaffRoles.blockIfNotStaff(event))
                    return;
                long evidenceToDelete = Long.parseLong(chunks[1]);

                try {
                    AWEvidence.removeFromDatabase(evidenceToDelete);
                    List<ItemComponent> components = event.getMessage().getActionRows().get(0).getComponents();
                    for (int i = 0; i < components.size(); i++) {
                        ItemComponent ic = components.get(i);
                        if (ic instanceof Button button) {
                            components.set(i, button.asDisabled());
                        }
                    }
                    event.getMessage().editMessageComponents(ActionRow.of(components)).queue();
                    event.reply("Successfully deleted the previous evidence.").queue();
                } catch (SQLException e) {
                    e.printStackTrace();
                    event.reply("An internal error occurred while trying to delete the evidence. Please try again later.\n```\n%s\n```".formatted(e.toString())).setEphemeral(true).queue();
                }
                break;
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("This bot can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String id = event.getModalId();

        if (id.isBlank()) return; // wtf

        String[] chunks = id.split("_");
        String action = chunks[0].toUpperCase(); // the first chunk is the "action" of the modal.

        switch (action) {
            case "MASSBAN": {
                long claimedModeratorId = Long.parseLong(chunks[1]);
                MassbanCommand.onMassbanModalSubmit(event, claimedModeratorId);
                break;
            }
            case "TICKET": {
                AWTicket ticket = AWTicketsManager.AWAITING_MODAL_RESPONSE.remove(id);
                if (ticket == null) {
                    event.reply("Something went wrong when trying to submit this ticket (did you wait too long on the form?). Please try again!").setEphemeral(true).queue();
                    return;
                }
                try {
                    // 2025 july update: migrate over to deferred reply
                    event.deferReply(true).queue();

                    // load the responses into the data structure
                    // will also reply to the event if the data is invalid
                    boolean success = ticket.loadFromModalResponse(event);

                    if (!success)
                        return; // stop here, input was invalid

                    // open the channel
                    ticket.createOrGetChannel(event.getJDA(), (channel -> {
                        ticket.afterTicketChannelCreated(channel);
                        AWTicketsManager.addNewTicketToCache(ticket);

                        String successMessage = "Your ticket has been successfully opened: " + channel.getAsMention();

                        // we're done with the ticket for now, update it in the database
                        try {
                            ticket.updateInDatabase();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            event.getInteraction().getHook().editOriginal(successMessage).queue();
                        }
                    }), (error -> event.getHook().editOriginal("Failed to open the ticket. Error from Discord: `%s`".formatted(error.getMessage() != null ? error.getMessage() : "No message provided")).queue()));
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    if (event.isAcknowledged()) {
                        if (!event.getInteraction().getHook().isExpired()) {
                            event.getInteraction().getHook().editOriginal("The Ability Wars database messed up somehow; try again later.").queue();
                        }
                    } else
                        event.reply("The Ability Wars database messed up somehow; try again later.").setEphemeral(true).queue();
                }
                break;
            }
            case "TICKET-ACTION": {
                String ticketAction = chunks[1];
                long ticketId = Long.parseLong(chunks[2]);
                String[] args = chunks.length > 3 ? Arrays.copyOfRange(chunks, 3, chunks.length) : new String[0];
                AWTicket ticketActionTicket = AWTicketsManager.getTicketFromCache(ticketId);
                if (ticketActionTicket == null) {
                    // ticket couldn't be found or is no longer open; do a no-op
                    event.deferEdit().queue();
                    return;
                }
                try {
                    ActionModals.sendToImplementation(ticketAction, args, event, ticketActionTicket);
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (event.isAcknowledged())
                        event.getInteraction().getHook().editOriginal("The Ability Wars database messed up somehow; try again later.").queue();
                    else
                        event.reply("The Ability Wars database messed up somehow; try again later.").setEphemeral(true).queue();
                }
                break;
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("ticket"))
            return;
        if (event.getSubcommandName() == null)
            return;
        if (!event.getSubcommandName().equalsIgnoreCase("modify"))
            return;
        if (event.getChannelType() != ChannelType.TEXT)
            return;

        var option = event.getFocusedOption();
        String optionName = option.getName();

        if (optionName.equalsIgnoreCase("ticket-property")) {
            // the `/ticket modify <ticket-property> <value>` command.
            TextChannel channel = event.getChannel().asTextChannel();
            AWTicket ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(channel);

            if (ticket == null) {
                event.replyChoices().queue();
                return;
            }

            final int MAX_CHOICES = OptionData.MAX_CHOICES;
            String[] possibleChoices = ticket.getPropertyChoices();
            String currentInput = event.getFocusedOption().getValue();

            if (possibleChoices.length <= MAX_CHOICES) {
                event.replyChoiceStrings(possibleChoices).queue();
                return;
            }

            LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
            String lowerInput = currentInput.toLowerCase();

            String[] choices = Arrays.stream(possibleChoices)
                    .sorted(Comparator.comparingInt(c -> levenshtein.apply(lowerInput, c.toLowerCase())))
                    .limit(MAX_CHOICES)
                    .toArray(String[]::new);

            event.replyChoiceStrings(choices).queue();
        }
    }
}
