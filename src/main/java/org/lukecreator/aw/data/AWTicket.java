package org.lukecreator.aw.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.data.tickets.*;
import org.lukecreator.aw.discord.AbilityWarsBot;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A ticket opened by a user in Ability Wars, for any reason.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public abstract class AWTicket {
    public static final long CATEGORY_ID_PLAYER_REPORTS = 1329635916083363911L;
    public static final long CATEGORY_ID_APPEALS = 1346983262610001984L;
    public static final long CATEGORY_ID_DISPUTES = 1272859910685851709L;
    public static final long CATEGORY_ID_BLACKLIST_APPEALS = 1212490640215253022L;
    private static AtomicLong nextId = null;
    public final long id;
    public final long ownerDiscordId;
    public final long openedTimestamp;
    public Long closedByDiscordId;
    public boolean isOpen;
    public String closeReason;
    protected JsonObject inputQuestionsRaw;
    private long discordChannelId;

    public AWTicket(long id,
                    long discordChannelId, long openedTimestamp,
                    boolean isOpen, String closeReason,
                    long closedByDiscordId, JsonObject inputQuestions,
                    long ownerDiscordId) {
        this.id = id;
        this.discordChannelId = discordChannelId;
        this.openedTimestamp = openedTimestamp;
        this.isOpen = isOpen;
        this.closeReason = closeReason;
        this.closedByDiscordId = closedByDiscordId;
        this.ownerDiscordId = ownerDiscordId;
        this.inputQuestionsRaw = inputQuestions;
    }

    /**
     * Sets up a new ticket and sends back a modal for the user to interact with.
     *
     * @param ticketType   The type of ticket to open.
     * @param currentGuild The guild the user is trying to open this ticket in.
     * @param ticketOwner  The owner of the ticket.
     * @return A modal to give to the user to finish the opening of the ticket. Never null; if an error occurs, it will be passed through an {@link IllegalArgumentException}.
     * @throws IllegalArgumentException If something about the user's input is wrong. Show the message to them.
     * @throws SQLException             If something went wrong inside the database. Check logs.
     */
    @NotNull
    public static Modal tryOpenNewTicket(AWTicket.Type ticketType, Guild currentGuild, UserSnowflake ticketOwner) throws IllegalArgumentException, SQLException {
        AWTicket ticket;
        long nextTicketID = AWTicket.nextAvailableTicketID();
        long openedTimestamp = System.currentTimeMillis();
        final long discordChannelId = -1L;
        final boolean isOpen = true;
        final long closedByDiscordId = -1L;

        ticket = switch (ticketType) {
            case PlayerReport ->
                    new AWPlayerReportTicket(nextTicketID, discordChannelId, openedTimestamp, isOpen, null, closedByDiscordId, null, ticketOwner.getIdLong());
            case BanAppeal ->
                    new AWBanAppealTicket(nextTicketID, discordChannelId, openedTimestamp, isOpen, null, closedByDiscordId, null, ticketOwner.getIdLong());
            case BanDispute ->
                    new AWBanDisputeTicket(nextTicketID, discordChannelId, openedTimestamp, isOpen, null, closedByDiscordId, null, ticketOwner.getIdLong());
            case BlacklistAppeal ->
                    new AWBlacklistAppealTicket(nextTicketID, discordChannelId, openedTimestamp, isOpen, null, closedByDiscordId, null, ticketOwner.getIdLong());
        };

        long guildId = currentGuild.getIdLong();

        if (ticketType.guildId != guildId) {
            AWTicket.decrementNextAvailableTicketID();
            throw new IllegalArgumentException("This ticket type isn't available in this server.");
        }

        int maxTicketsPerUser = ticketType.maxTicketsPerUser;
        int ticketsThisUserHasOpen = 0;
        for (AWTicket openTicket : AWTicketsManager.getOpenTickets()) {
            if (openTicket.type() != ticketType)
                continue;
            if (openTicket.ownerDiscordId == ticketOwner.getIdLong())
                ticketsThisUserHasOpen++;
        }
        if (ticketsThisUserHasOpen >= maxTicketsPerUser) {
            AWTicket.decrementNextAvailableTicketID();
            throw new IllegalArgumentException("You have too many tickets open! Please wait until your previous ticket(s) are closed.");
        }

        // check that we can actually fit another ticket in the category
        final int maxTickets = 50;
        Category ticketCategory = currentGuild.getCategoryById(ticketType.channelCategoryId);
        if (ticketCategory == null) {
            AWTicket.decrementNextAvailableTicketID();
            throw new IllegalArgumentException("So... basically... the category this ticket is supposed to go in doesn't exist. Please wait for the developers to fix this!");
        }
        int ticketsInCategory = ticketCategory.getTextChannels().size();
        if (ticketsInCategory >= maxTickets) {
            AWTicket.decrementNextAvailableTicketID();
            throw new IllegalArgumentException("Tickets are currently full (`" + maxTickets + "` in queue). Please wait a bit before trying again!");
        }

        Modal modal = ticket.createInputModal(nextTicketID);

        if (modal == null) {
            AWTicket.decrementNextAvailableTicketID();
            throw new IllegalArgumentException("Something went wrong when trying to open a ticket. Try again in a couple minutes. If this keeps happening, contact a developer.");
        }

        // show the modal to the user start waiting for a response on it.
        AWTicketsManager.AWAITING_MODAL_RESPONSE.put(modal.getId(), ticket);
        return modal;
    }

    public static AWTicket createBasedOnType(Type type, long id,
                                             long discordChannelId, long openedTimestamp,
                                             boolean isOpen, String closeReason,
                                             long closedByDiscordId, JsonObject inputQuestions,
                                             long ownerDiscordId) {
        switch (type) {
            case PlayerReport -> {
                return new AWPlayerReportTicket(id, discordChannelId, openedTimestamp,
                        isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
            }
            case BanAppeal -> {
                return new AWBanAppealTicket(id, discordChannelId, openedTimestamp,
                        isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
            }
            case BanDispute -> {
                return new AWBanDisputeTicket(id, discordChannelId, openedTimestamp,
                        isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
            }
            case BlacklistAppeal -> {
                return new AWBlacklistAppealTicket(id, discordChannelId, openedTimestamp,
                        isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
            }
            default -> throw new RuntimeException("Tried to create a ticket with an invalid type. (type: %d, id: %d)"
                    .formatted(type.id, id));
        }
    }

    private static void initializeNextAvailableTicketID() throws SQLException {
        // fetch the highest ticket ID from the database
        var statement = AWDatabase.connection.prepareStatement("SELECT MAX(ticket_id) FROM tickets");
        try (var results = statement.executeQuery()) {
            if (results.next()) {
                nextId = new AtomicLong(results.getLong(1) + 1);
                return;
            }
        }
        nextId = new AtomicLong(1L);
    }

    /**
     * Retrieves the next available ticket ID.
     * <p>
     * If the ticket ID counter has not been initialized, it initializes the counter
     * by fetching the highest ticket ID from the database and sets the starting value.
     * Each subsequent call increments the ticket ID counter and returns the new value.
     *
     * @return The next available ticket ID as a long value.
     * @throws SQLException If an error occurs while initializing the ticket ID counter.
     */
    public static long nextAvailableTicketID() throws SQLException {
        if (nextId == null)
            initializeNextAvailableTicketID();
        return nextId.getAndIncrement();
    }

    /**
     * Decrements the next available ticket ID counter.
     * <p>
     * If the ticket ID counter has not been initialized, it initializes the counter
     * by fetching the highest ticket ID from the database and setting the starting value.
     * This method ensures that the next available ticket ID is decremented safely.
     *
     * @throws SQLException If an error occurs while initializing the ticket ID counter.
     */
    public static void decrementNextAvailableTicketID() throws SQLException {
        if (nextId == null)
            initializeNextAvailableTicketID();
        nextId.decrementAndGet();
    }

    /**
     * Loads/retrieves a ticket from the database that has the given ID.
     *
     * @param id The ID of the ticket to retrieve
     * @return {@code null} if there are no tickets in the database with the input ID.
     * @throws SQLException If something went wrong with the database internally.
     */
    public static AWTicket loadFromDatabase(long id) throws SQLException {
        try (var statement = AWDatabase.connection.prepareStatement("""
                SELECT discord_channel_id, type, opened_timestamp, is_open, close_reason, closed_by, input_questions, owner_discord_id FROM tickets
                WHERE ticket_id = ?""")) {
            statement.setLong(1, id);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                long discordChannelId = results.getLong(1);
                Type type = Type.fromId(results.getInt(2));

                if (type == null) {
                    throw new RuntimeException("Loaded ticket without a valid type. (type: %d, id: %d)"
                            .formatted(results.getInt(2), id));
                }

                long openedTimestamp = results.getLong(3);
                boolean isOpen = results.getBoolean(4);
                String closeReason = results.getString(5);
                long closedByDiscordId = results.getLong(6);
                String inputQuestionsRaw = results.getString(7);
                long ownerDiscordId = results.getLong(8);
                JsonObject inputQuestions = JsonParser.parseString(inputQuestionsRaw).getAsJsonObject();

                AWTicket loadedTicket = createBasedOnType(type, id, discordChannelId, openedTimestamp, isOpen,
                        closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
                loadedTicket.processInputQuestionsJSON(inputQuestions);
                return loadedTicket;
            }
        }
    }

    /**
     * Retrieves an array of AWTicket objects by executing the provided PreparedStatement.
     *
     * @param statement the PreparedStatement used to query the database for ticket information
     * @return an array of AWTicket objects retrieved and sorted by their opened timestamp in descending order
     * @throws SQLException if a database access error occurs or the statement execution fails
     */
    @NotNull
    private static AWTicket[] getAWTicketsByStatement(PreparedStatement statement) throws SQLException {
        try (var results = statement.executeQuery()) {
            List<AWTicket> tickets = new ArrayList<>();
            while (results.next()) {
                long ticketId = results.getLong(1);
                AWTicket ticket = loadFromDatabase(ticketId);
                tickets.add(ticket);
            }
            tickets.sort((a, b) -> Long.compare(b.openedTimestamp, a.openedTimestamp));
            return tickets.toArray(new AWTicket[0]);
        }
    }

    /**
     * Loads all tickets associated with the specified owner from the database.
     *
     * @param owner The UserSnowflake object representing the owner whose tickets are to be loaded.
     * @param limit The maximum number of tickets to load. Keep this low because it fully loads every ticket.
     * @return An array of AWTicket objects sorted by their opened timestamps in descending order.
     * @throws SQLException If a database access error occurs.
     */
    public static AWTicket[] loadByOwner(UserSnowflake owner, int limit) throws SQLException {
        long ownerId = owner.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT ticket_id FROM tickets WHERE owner_discord_id = ? LIMIT ?")) {
            statement.setLong(1, ownerId);
            statement.setInt(2, limit);
            return getAWTicketsByStatement(statement);
        }
    }

    /**
     * Loads all tickets associated with the specified owner from the database.
     *
     * @param owner The UserSnowflake object representing the owner whose tickets are to be loaded.
     * @param limit The maximum number of tickets to load. Keep this low because it fully loads every ticket.
     * @param type  The type of ticket to restrict the lookup to.
     * @return An array of AWTicket objects sorted by their opened timestamps in descending order.
     * @throws SQLException If a database access error occurs.
     */
    public static AWTicket[] loadByOwner(UserSnowflake owner, int limit, AWTicket.Type type) throws SQLException {
        long ownerId = owner.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT ticket_id FROM tickets WHERE owner_discord_id = ? AND type = ? LIMIT ?")) {
            statement.setLong(1, ownerId);
            statement.setInt(2, type.id);
            statement.setInt(3, limit);
            return getAWTicketsByStatement(statement);
        }
    }

    /**
     * Loads all tickets closed by the specified user from the database.
     *
     * @param closer The UserSnowflake object representing the ticket closer to use for the lookup.
     * @return An array of AWTicket objects sorted by their opened timestamps in descending order.
     * @throws SQLException If a database access error occurs.
     */
    public static AWTicket[] loadByCloser(UserSnowflake closer, int limit) throws SQLException {
        long closerId = closer.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT ticket_id FROM tickets WHERE is_open = false AND closed_by = ? LIMIT ?")) {
            statement.setLong(1, closerId);
            statement.setInt(2, limit);
            return getAWTicketsByStatement(statement);
        }
    }

    /**
     * Loads all tickets closed by the specified user from the database.
     *
     * @param closer The UserSnowflake object representing the ticket closer to use for the lookup.
     * @param type   The type of ticket to restrict the lookup to.
     * @return An array of AWTicket objects sorted by their opened timestamps in descending order.
     * @throws SQLException If a database access error occurs.
     */
    public static AWTicket[] loadByCloser(UserSnowflake closer, int limit, AWTicket.Type type) throws SQLException {
        long closerId = closer.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT ticket_id FROM tickets WHERE is_open = false AND closed_by = ? AND type = ? LIMIT ?")) {
            statement.setLong(1, closerId);
            statement.setInt(2, type.id);
            statement.setInt(3, limit);
            return getAWTicketsByStatement(statement);
        }
    }

    /**
     * Counts the number of tickets associated with the specified owner from the database.
     *
     * @param owner The UserSnowflake object representing the owner whose tickets are to be counted.
     * @return The number of tickets associated with the specified owner.
     */
    public static int countByOwner(UserSnowflake owner) throws SQLException {
        long ownerId = owner.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT COUNT(*) FROM tickets WHERE owner_discord_id = ?")) {
            statement.setLong(1, ownerId);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return 0;
                }
                return results.getInt(1);
            }
        }
    }

    /**
     * Counts the number of tickets associated with the specified owner from the database.
     *
     * @param owner The UserSnowflake object representing the owner whose tickets are to be counted.
     * @param type  The type of ticket to restrict the count to.
     * @return The number of tickets associated with the specified owner.
     */
    public static int countByOwner(UserSnowflake owner, AWTicket.Type type) throws SQLException {
        long ownerId = owner.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT COUNT(*) FROM tickets WHERE owner_discord_id = ? AND type = ?")) {
            statement.setLong(1, ownerId);
            statement.setInt(2, type.id);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return 0;
                }
                return results.getInt(1);
            }
        }
    }

    /**
     * Counts the number of tickets that are closed by a specific user.
     *
     * @param closer The UserSnowflake object representing the user who closed the tickets.
     * @return The total count of tickets closed by the specified user.
     * @throws SQLException If a database access error occurs during the query.
     */
    public static int countByCloser(UserSnowflake closer) throws SQLException {
        long closerId = closer.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT COUNT(*) FROM tickets WHERE is_open = false AND closed_by = ?")) {
            statement.setLong(1, closerId);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return 0;
                }
                return results.getInt(1);
            }
        }
    }

    /**
     * Counts the number of tickets that are closed by a specific user.
     *
     * @param closer The UserSnowflake object representing the user who closed the tickets.
     * @param type   The type of ticket to restrict the count to.
     * @return The total count of tickets closed by the specified user.
     * @throws SQLException If a database access error occurs during the query.
     */
    public static int countByCloser(UserSnowflake closer, AWTicket.Type type) throws SQLException {
        long closerId = closer.getIdLong();
        try (var statement = AWDatabase.connection.prepareStatement("SELECT COUNT(*) FROM tickets WHERE is_open = false AND closed_by = ? AND type = ?")) {
            statement.setLong(1, closerId);
            statement.setInt(2, type.id);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return 0;
                }
                return results.getInt(1);
            }
        }
    }

    /**
     * Set a dynamic property in this ticket by name. Used by the {@code /ticket modify ...} and serves
     * as an interface to let staff modify the ticket dynamically.
     *
     * @param key   The key of the property to change. Possible values are defined in {@link #getPropertyChoices()}
     * @param value The unvalidated value to set the property to. May be null if the property was unspecified.
     * @param event The event to reply to as if a command was run.
     * @throws SQLException If something went wrong with the database internally.
     * @see org.lukecreator.aw.discord.commands.TicketManageCommand
     */
    public abstract void setProperty(@NotNull String key, @Nullable String value, SlashCommandInteractionEvent event) throws SQLException;

    /**
     * Returns a list of available property names for the user to use when typing the {@code /ticket modify ...} command.
     *
     * @return The possible choices.
     * @see org.lukecreator.aw.discord.commands.TicketManageCommand
     */
    public abstract String[] getPropertyChoices();

    /**
     * Updates this ticket in the database with its current state. This should generally be called when any change is
     * made and the ticket is done being updated for the time being.
     *
     * @throws SQLException If something went wrong with the database internally.
     */
    public void updateInDatabase() throws SQLException {
        try (var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO tickets (ticket_id, discord_channel_id, type, opened_timestamp, is_open, close_reason, closed_by, input_questions, owner_discord_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(ticket_id) DO UPDATE SET
                                                                                        discord_channel_id = excluded.discord_channel_id,
                                                                                        type = excluded.type,
                                                                                        opened_timestamp = excluded.opened_timestamp,
                                                                                        is_open = excluded.is_open,
                                                                                        close_reason = excluded.close_reason,
                                                                                        closed_by = excluded.closed_by,
                                                                                        input_questions = excluded.input_questions,
                                                                                        owner_discord_id = excluded.owner_discord_id""")) {
            statement.setLong(1, this.id);
            statement.setLong(2, this.discordChannelId);
            statement.setInt(3, this.type().id);
            statement.setLong(4, this.openedTimestamp);
            statement.setBoolean(5, this.isOpen);
            statement.setString(6, this.closeReason);
            statement.setLong(7, this.closedByDiscordId);
            statement.setString(8, this.getInputQuestionsJSON().toString());
            statement.setLong(9, this.ownerDiscordId);
            statement.executeUpdate();
        }
    }

    public long getDiscordChannelId() {
        return this.discordChannelId;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    /**
     * Returns the ticket {@link AWTicket.Type} that this ticket is.
     */
    public abstract Type type();

    /**
     * Returns the JSON representation of this Ticket's input questions.
     */
    public abstract JsonObject getInputQuestionsJSON();

    /**
     * Processes the given JSON object into this Ticket's input questions, kind of like a fancy setter.
     *
     * @param json The JSON to process.
     */
    public abstract void processInputQuestionsJSON(JsonObject json);

    /**
     * Called after the cache of open tickets has fully loaded.
     */
    public abstract void afterCacheLoaded();

    /**
     * Called after a {@link TextChannel} is created for this ticket and the initial message has been sent.
     *
     * @param channel The newly created text channel.
     */
    public abstract void afterTicketChannelCreated(TextChannel channel);

    /**
     * Called after the {@link #getInitialMessageEmbeds(JDA)} message has completed sending.
     *
     * @param channel The ticket channel.
     * @param message The newly created message, based off the output of {@link #getInitialMessageEmbeds(JDA)}.
     */
    public abstract void afterInitialMessageSent(TextChannel channel, Message message);

    /**
     * Called when a message is sent in this ticket's channel.
     *
     * @param event The message event.
     */
    public abstract void onMessageReceived(MessageReceivedEvent event);

    /**
     * Create a Discord Modal for the user to input their answers to the opening questions.
     *
     * @return The created Modal.
     */
    public abstract Modal createInputModal(long newTicketId);

    /**
     * Gets an array of {@link TicketAction}s which can be taken on this ticket.
     */
    public abstract TicketAction[] getAvailableActions();

    /**
     * Get the embed(s) that will be sent in the first message in the channel for this ticket.
     */
    public abstract List<MessageEmbed> getInitialMessageEmbeds(JDA jda);

    /**
     * Handle an interaction with this ticket.
     *
     * @param actionId The action ID.
     * @param event    The event.
     */
    public abstract void handleAction(String actionId, ButtonInteractionEvent event);

    /**
     * Processes the user's responses to the modal returned from {@link #createInputModal(long)} into this Ticket's input questions, kind of like a fancy setter.
     * <p>
     * This method CANNOT reply to the event with a message, it must edit the deferred reply.
     * <ul>
     *     <li>If the method returns <c>true</c>, the caller will likely be editing the message immediately after, so a defer is recommended.</li>
     *     <li>If the method returns <c>false</c>, the caller should halt and not modify the message further.</li>
     * </ul>
     *
     * @param event The {@link ModalInteractionEvent} generated by the submission of the modal.
     * @return True if the input is correct and the ticket should continue opening. If false, stop there and cancel.
     */
    public abstract boolean loadFromModalResponse(ModalInteractionEvent event) throws SQLException;

    /**
     * If it doesn't yet exist, creates a channel for this ticket and assigns {@link #discordChannelId}.
     * Does NOT update the ticket in the database! The initial message will be sent in the channel.
     * <p>
     * If the channel does exist, the consumer is sent the existing channel.
     * <p>
     * If something goes wrong, nothing will happen and {@link #discordChannelId} will stay the same.
     *
     * @param jda             The JDA instance driving the operation.
     * @param successConsumer If not null, the consumer to run when the channel is successfully created/found.
     * @param failureConsumer If not null, the consumer to run if the channel couldn't be created (category limit?)
     */
    public void createOrGetChannel(JDA jda, Consumer<TextChannel> successConsumer, Consumer<Throwable> failureConsumer) {
        Type ticketType = this.type();
        Guild guild = jda.getGuildById(ticketType.guildId);

        if (guild == null) {
            System.out.println("Strange problem: Cannot find guild " + ticketType.guildId + " for ticket type " + ticketType.identifier + " (id: " + this.id + ")");
            return;
        }

        if (this.discordChannelId != 0L && this.discordChannelId != -1L) {
            TextChannel tc = guild.getTextChannelById(this.discordChannelId);
            if (tc != null && successConsumer != null) {
                successConsumer.accept(tc);
            }
            return;
        }

        Category category = guild.getCategoryById(ticketType.channelCategoryId);

        if (category == null) {
            System.out.println("Strange problem: Cannot find category " + ticketType.channelCategoryId + " for ticket type " + ticketType.identifier + " (id: " + this.id + ")");
            return;
        }

        EnumSet<Permission> allowedPermissions = EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_EMBED_LINKS);

        category.createTextChannel(ticketType.channelPrefix + this.id)
                .addMemberPermissionOverride(this.ownerDiscordId, allowedPermissions, Collections.emptySet())
                .queue(whenDone -> {

                    // collect the action components
                    TicketAction[] availableActions = this.getAvailableActions();
                    Button[] buttons = new Button[availableActions.length];
                    for (int i = 0; i < availableActions.length; i++)
                        buttons[i] = availableActions[i].getButton();

                    List<MessageEmbed> embeds = this.getInitialMessageEmbeds(jda);
                    if (embeds.isEmpty()) {
                        embeds.add(new EmbedBuilder()
                                /* fallback embed so that we can have something to attach the components to */
                                .setTitle("Ticket " + this.id)
                                .setDescription("Opened <t:%s:f> by <@%s>".formatted(this.openedTimestamp / 1000, this.ownerDiscordId))
                                .build());
                    }

                    var action = whenDone.sendMessageEmbeds(embeds);

                    final int maxPerRow = Component.Type.BUTTON.getMaxPerRow();
                    final int maxRows = Message.MAX_COMPONENT_COUNT;
                    final int maxComponents = maxRows * maxPerRow;
                    if (buttons.length > maxComponents) {
                        // too many components!
                        throw new RuntimeException("Too many components in this ticket (type " + ticketType.id + ")! Got " + buttons.length + " actions?");
                    }

                    // distribute buttons into the message action
                    if (buttons.length > maxPerRow) {
                        // we need more than one action row
                        int minimumRows = buttons.length / maxPerRow;
                        if (buttons.length % maxPerRow != 0)
                            minimumRows++;

                        int b = 0;

                        // split buttons evenly between action rows
                        int perRow = buttons.length / minimumRows;
                        int extraButtons = buttons.length % minimumRows;

                        for (int i = 0; i < minimumRows; i++) {
                            int onThisRow = perRow;
                            if (extraButtons > 0) {
                                onThisRow++;
                                extraButtons--;
                            }
                            Button[] row = new Button[onThisRow];
                            for (int j = 0; j < onThisRow; j++)
                                row[j] = buttons[b++];
                            action.addActionRow(row);
                        }
                    } else {
                        // will fit on one action row
                        action.setActionRow(buttons);
                    }

                    // send the message
                    action.queue(msg -> this.afterInitialMessageSent(whenDone, msg));

                    // post the evidence in the channel so it embeds
                    if (this instanceof AWPlayerReportTicket thisAsReport) {
                        if (thisAsReport.hasEvidence()) {
                            String url = thisAsReport.getEvidenceURL();
                            whenDone.sendMessage(url).queue();
                        }
                    }

                    // assign to the ticket
                    this.discordChannelId = whenDone.getIdLong();

                    // run the caller's consumer
                    if (successConsumer != null)
                        successConsumer.accept(whenDone);
                }, failureConsumer);
    }

    /**
     * Close this ticket. It will be actually closed at some point in the future, as this sends multiple Discord API requests.
     *
     * @param jda                 The API instance to use for Discord operations.
     * @param closedByUser        The user that closed the ticket.
     * @param closeReason         The user-friendly description of why the ticket was closed.
     * @param onSuccess           If not null, the consumer to run when the ticket is fully, successfully closed.
     * @param additionalUserEmbed If not null, an additional embed to append to the end of the message to the ticket owner.
     * @throws SQLException If something went wrong in the database.
     */
    public void close(JDA jda, User closedByUser, String closeReason, @Nullable Consumer<JDA> onSuccess, @Nullable MessageEmbed additionalUserEmbed) throws SQLException {
        if (!this.isOpen) {
            // check and make sure the channel's gone
            Guild guild = jda.getGuildById(this.type().guildId);
            if (this.discordChannelId != 0L && this.discordChannelId != -1L) {
                if (guild != null) {
                    TextChannel channel = guild.getTextChannelById(this.discordChannelId);
                    if (channel != null) {
                        channel.delete().queue(whenDone -> {
                            if (onSuccess != null)
                                onSuccess.accept(jda);
                        });
                    }
                }
            }
            return;
        }

        this.isOpen = false;
        this.closeReason = closeReason;
        this.closedByDiscordId = closedByUser.getIdLong();
        this.updateInDatabase();

        // remove from the ticket manager cache
        AWTicketsManager.removeTicketFromCache(this.id);

        // delete the ticket channel
        Guild guild = jda.getGuildById(this.type().guildId);
        if (this.discordChannelId != 0L && this.discordChannelId != -1L) {
            if (guild != null) {
                TextChannel channel = guild.getTextChannelById(this.discordChannelId);
                if (channel != null) {
                    channel.delete().queue(whenDone -> {
                        if (onSuccess != null)
                            onSuccess.accept(jda);
                    });
                }
            }
        }

        // message the user
        jda.openPrivateChannelById(this.ownerDiscordId).queue(whenDone -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setTitle("Your ticket has been closed! Reason:")
                    .setDescription(closeReason == null ? "-# (unspecified)" : closeReason)
                    .addField("Closed By", "%s (%s)".formatted(closedByUser.getAsMention(), closedByUser.getName()), true)
                    .addField("Server", guild == null ? "Unknown" : guild.getName(), true)
                    .build();
            if (additionalUserEmbed != null) {
                whenDone.sendMessageEmbeds(embed, additionalUserEmbed).queue(null, f -> {
                });
            } else {
                whenDone.sendMessageEmbeds(embed).queue(null, f -> {
                });
            }
            return;
        });
    }

    /**
     * Like {@link #close(JDA, User, String, Consumer, MessageEmbed)}, but doesn't alert the user and doesn't complain if, for example,
     * the channel is missing. This is meant as a way of removing the ticket from the cache/db if it's broken.
     *
     * @param jda The API to use.
     * @throws SQLException If something went wrong in the database.
     */
    public void cleanup(JDA jda) throws SQLException {
        // remove the channel if it exists.
        Guild guild = jda.getGuildById(this.type().guildId);
        if (guild != null) {
            TextChannel ticketChannel = guild.getTextChannelById(this.discordChannelId);
            if (ticketChannel != null) {
                ticketChannel.delete().queue(null, ignored -> { /* silently fail */ });
            }
        }

        this.isOpen = false;
        this.closeReason = "Ticket died. Please recreate.";
        this.closedByDiscordId = jda.getSelfUser().getIdLong();
        this.updateInDatabase();
        AWTicketsManager.removeTicketFromCache(this.id);
    }

    /**
     * Deletes the current ticket's data from the database entirely, including evidence links.
     *
     * @throws SQLException If a database access error occurs while executing the SQL statements.
     */
    public void removeFromDatabase() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("DELETE FROM tickets WHERE ticket_id = ?");
        statement.setLong(1, this.id);
        statement.executeUpdate();

        statement = AWDatabase.connection.prepareStatement("DELETE FROM ticket_evidence_link WHERE ticket_id = ?");
        statement.setLong(1, this.id);
        statement.executeUpdate();
    }


    /**
     * A type of ticket.
     */
    public enum Type {
        PlayerReport("player-report", "Player Report", 0xA0, 3,
                "report-", "Report a player for breaking the rules", AbilityWarsBot.AW_GUILD_ID, CATEGORY_ID_PLAYER_REPORTS),
        BanAppeal("ban-appeal", "Ban Appeal", 0xB0, 1,
                "appeal-", "Appeal a ban for exploiting", AbilityWarsBot.AW_APPEALS_GUILD_ID, CATEGORY_ID_APPEALS),
        BanDispute("ban-dispute", "Ban Dispute", 0xB1, 1,
                "dispute-", "Dispute a ban that was a mistake by staff", AbilityWarsBot.AW_APPEALS_GUILD_ID, CATEGORY_ID_DISPUTES),
        BlacklistAppeal("blacklist-appeal", "Blacklist Appeal", 0xB2, 1,
                "blacklist-appeal-", "Appeal a blacklist from report tickets", AbilityWarsBot.AW_APPEALS_GUILD_ID, CATEGORY_ID_BLACKLIST_APPEALS);

        public static final Type[] MAIN_SERVER_TYPES = Arrays
                .stream(Type.values())
                .filter(Type::isMainServer)
                .toArray(Type[]::new);
        public static final Type[] APPEALS_SERVER_TYPES = Arrays
                .stream(Type.values())
                .filter(Type::isAppealsServer)
                .toArray(Type[]::new);
        public static final Command.Choice[] AS_COMMAND_CHOICES = Arrays
                .stream(Type.values())
                .map(type -> new Command.Choice(type.identifier, type.identifier))
                .toArray(Command.Choice[]::new);
        public static final long[] TICKET_CATEGORY_IDS = Arrays
                .stream(Type.values())
                .mapToLong(t -> t.channelCategoryId)
                .toArray();
        public final String identifier;
        public final String title;
        public final int id;
        public final int maxTicketsPerUser;
        public final String channelPrefix;
        public final String description;
        public final long guildId;
        public final long channelCategoryId;

        /**
         * The unique ID of this ticket type. Used in the database, so don’t change these!
         * <p>
         * Format is as follows:
         * <ul>
         *  <li>0xA... main server ticket types</li>
         *  <li>0xB... appeals server ticket types</li>
         * </ul>
         */
        Type(String identifier, String title, int id, int maxTicketsPerUser, String channelPrefix, String description, long guildId, long channelCategoryId) {
            this.identifier = identifier;
            this.title = title;
            this.id = id;
            this.maxTicketsPerUser = maxTicketsPerUser;
            this.channelPrefix = channelPrefix;
            this.description = description;
            this.guildId = guildId;
            this.channelCategoryId = channelCategoryId;
        }

        /**
         * Checks if the given TextChannel's parent category is part of the valid ticket categories.
         *
         * @param channel The TextChannel to check.
         * @return true if the parent category of the channel matches any of the defined ticket category IDs,
         * false otherwise.
         */
        public static boolean isInTicketCategory(ICategorizableChannel channel) {
            Category parent = channel.getParentCategory();
            if (parent == null)
                return false;
            for (long id : TICKET_CATEGORY_IDS) {
                if (parent.getIdLong() == id)
                    return true;
            }
            return false;
        }

        public static Type fromId(int id) {
            for (Type type : Type.values()) {
                if (type.id == id)
                    return type;
            }
            return null;
        }

        public static Type fromIdentifier(String identifier) {
            for (Type type : Type.values()) {
                if (type.identifier.equals(identifier))
                    return type;
            }
            return null;
        }

        /**
         * Returns if this Ticket type can be used in the main Discord server.
         */
        public boolean isMainServer() {
            return this.id >= 0xA0 && this.id <= 0xAF;
        }

        /**
         * Returns if this Ticket type can be used in the appeals Discord server.
         */
        public boolean isAppealsServer() {
            return this.id >= 0xB0 && this.id <= 0xBF;
        }

        public String getCreationModalCustomId() {
            return "ticket_" + this.identifier;
        }
    }
}
