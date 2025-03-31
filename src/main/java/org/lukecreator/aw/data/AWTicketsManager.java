package org.lukecreator.aw.data;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.AWDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds a cache of open tickets and provides a method for loading open tickets from the database.
 */
public class AWTicketsManager {
    public static final HashMap<String, AWTicket> AWAITING_MODAL_RESPONSE = new HashMap<>();
    private static final HashMap<Long, AWTicket> OPEN_TICKETS_BY_ID = new HashMap<>();
    private static final HashMap<Long, AWTicket> OPEN_TICKETS_BY_DISCORD_CHANNEL_ID = new HashMap<>();

    /**
     * Retrieves a collection of all open tickets currently stored in the cache.
     *
     * @return A collection of open tickets, sourced from the cache.
     */
    public static Collection<AWTicket> getOpenTickets() {
        return OPEN_TICKETS_BY_ID.values();
    }

    /**
     * Loads all open tickets from the database into the cache. Discards the previous contents of the cache, if any.
     * This can use the database for quite a while, since it needs {@code N+1} individual accesses to load all the tickets.
     *
     * @throws SQLException If something went wrong with the database for some reason.
     */
    public static void loadTicketsFromDatabase() throws SQLException {
        OPEN_TICKETS_BY_ID.clear();
        OPEN_TICKETS_BY_DISCORD_CHANNEL_ID.clear();

        // collect a list of open ticket IDs
        try (var statement = AWDatabase.connection.prepareStatement("""
                SELECT ticket_id from tickets where is_open = TRUE""")) {
            try (var results = statement.executeQuery()) {
                while (results.next()) {
                    var ticketId = results.getLong(1);
                    AWTicket ticket = AWTicket.loadFromDatabase(ticketId);
                    if (ticket == null)
                        continue;
                    OPEN_TICKETS_BY_ID.put(ticketId, ticket);
                    OPEN_TICKETS_BY_DISCORD_CHANNEL_ID.put(ticket.getDiscordChannelId(), ticket);
                }
            }
        }

        // any additional necessary setup for specific ticket types
        for (AWTicket ticket : getOpenTickets()) {
            ticket.afterCacheLoaded();
        }
    }

    /**
     * Runs a fairly intensive task which cleans up any "dead tickets".
     * These almost always exist due to a bug and take up space preventing users from fully filling up the category.
     * <p>
     * For a ticket to be dead, it must satisfy one of the following conditions:
     * <ul>
     *     <li>Its {@link AWTicket#getDiscordChannelId()} doesn't resolve to a valid channel.</li>
     *     <li>The channel is missing the primary bot message, making it impossible to resolve.</li>
     * </ul>
     * <p>
     * This should be run maybe once on bot start and be available via a command in case something breaks. This function
     * is also fully synchronous and uses quite a few requests (a lot), so run it sparingly!
     *
     * @param jda                   The API instance to initiate the deletions from.
     * @param afterCollectedTickets If not null, this will be called before the cleanup begins. The number of tickets that will be cleaned up is passed as the argument.
     * @param afterDone             If not null, this will be called after the cleanup is completed. A non-null list of strings describes any errors that occurred during the process.
     */
    public static void deadTicketCleanup(JDA jda, @Nullable Consumer<Integer> afterCollectedTickets, @Nullable Consumer<List<String>> afterDone) {
        List<String> errors = new ArrayList<>();
        List<AWTicket> deadTickets = new ArrayList<>();

        final long selfUserId = jda.getSelfUser().getIdLong();

        // collect dead tickets
        for (AWTicket ticket : OPEN_TICKETS_BY_ID.values()) {
            long ticketId = ticket.id;
            AWTicket.Type type = ticket.type();
            long guildId = type.guildId;
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                errors.add("Unavailable guild %d for ticket %d".formatted(guildId, ticketId));
                continue;
            }

            long channelId = ticket.getDiscordChannelId();
            TextChannel channel = guild.getTextChannelById(channelId);

            if (channel == null) {
                deadTickets.add(ticket);
                continue;
            }

            // channel does exist, now scan for the primary bot message
            List<Message> pinnedMessages = channel.retrievePinnedMessages().complete();
            if (pinnedMessages.isEmpty()) {
                deadTickets.add(ticket);
                continue;
            }
            boolean anyByBot = false;
            for (Message pinnedMessage : pinnedMessages) {
                if (pinnedMessage.getAuthor().getIdLong() == selfUserId) {
                    anyByBot = true;
                    break;
                }
            }
            if (!anyByBot)
                deadTickets.add(ticket);
        }

        if (afterCollectedTickets != null)
            afterCollectedTickets.accept(deadTickets.size());

        // remove them silently
        for (AWTicket deadTicket : deadTickets) {
            try {
                deadTicket.cleanup(jda, null);
            } catch (SQLException e) {
                errors.add("Failed to cleanup dead ticket %d due to database error: %s".formatted(deadTicket.id, e.getMessage()));
            }
        }

        if (afterDone != null)
            afterDone.accept(errors);
    }

    /**
     * Adds a new ticket to the manager. This method DOESN'T commit the ticket to the database, only the cache. Use {@link AWTicket#updateInDatabase()} once you're ready.
     *
     * @param ticket The ticket to add.
     */
    public static void addNewTicketToCache(AWTicket ticket) {
        OPEN_TICKETS_BY_ID.put(ticket.id, ticket);
        OPEN_TICKETS_BY_DISCORD_CHANNEL_ID.put(ticket.getDiscordChannelId(), ticket);
    }

    /**
     * Retrieves a ticket from the cache based on the associated Ticket ID.
     *
     * @param ticketID The ticket ID associated with the ticket.
     * @return The ticket associated with the specified Ticket ID, or {@code null} if no ticket is found.
     */
    public static AWTicket getTicketFromCache(long ticketID) {
        return OPEN_TICKETS_BY_ID.get(ticketID);
    }

    /**
     * Retrieves a ticket from the cache based on the provided Discord text channel.
     *
     * @param textChannel The Discord text channel associated with the ticket.
     * @return The ticket associated with the specified text channel, or {@code null} if no ticket is found.
     */
    public static @Nullable AWTicket getTicketFromCacheByDiscordChannel(TextChannel textChannel) {
        return getTicketFromCacheByDiscordChannelId(textChannel.getIdLong());
    }

    /**
     * Retrieves a ticket from the cache based on the associated Discord channel ID.
     *
     * @param discordChannelId The Discord channel ID associated with the ticket.
     * @return The ticket associated with the specified Discord channel ID, or {@code null} if no ticket is found.
     */
    public static AWTicket getTicketFromCacheByDiscordChannelId(long discordChannelId) {
        return OPEN_TICKETS_BY_DISCORD_CHANNEL_ID.get(discordChannelId);
    }

    /**
     * Removes the ticket associated with the given Ticket ID from the cache.
     *
     * @param ticketID The ID of the ticket to be removed from the cache.
     */
    public static void removeTicketFromCache(long ticketID) {
        OPEN_TICKETS_BY_ID.remove(ticketID);
        OPEN_TICKETS_BY_DISCORD_CHANNEL_ID.remove(ticketID);
    }

}
