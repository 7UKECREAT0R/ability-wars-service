package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.BanRequest;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class MassbanCommand extends BotCommand {
    public MassbanCommand() {
        super("aw-massban", "(staff only) Ban multiple users permanently from Ability Wars.");
    }

    public static void onMassbanModalSubmit(ModalInteractionEvent e, long claimedRobloxId) {
        long userId = e.getUser().getIdLong();

        try {
            Long linkedRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(userId);
            if (linkedRobloxId == null) {
                e.reply(e.getUser().getAsMention() + ", couldn't get your Roblox ID from your Discord ID. If you haven't already, get your accounts linked with an administrator.").queue();
                return;
            }
            if (linkedRobloxId != claimedRobloxId) {
                e.reply(e.getUser().getAsMention() + ", it seems like someone else opened that modal? This definitely isn't supposed to happen.").queue();
                return;
            }

            ModalMapping inputMapping = e.getInteraction().getValue("bans");
            if (inputMapping == null) {
                e.reply(e.getUser().getAsMention() + ", something went wrong when trying to get your input. Try again in a couple of minutes.").queue();
                return;
            }
            String input = inputMapping.getAsString();
            if (input.isBlank()) {
                e.reply(e.getUser().getAsMention() + ", input was blank.").queue();
                return;
            }
            String[] _banList = input.split("\n");
            if (_banList.length % 2 != 0) {
                e.reply(e.getUser().getAsMention() + ", found odd number of ban/reason pairs, so the massban will not be run.").queue();
                return;
            }
            String[] banList = new String[_banList.length];
            for (int i = 0; i < _banList.length; i++)
                banList[i] = _banList[i].strip();

            int _bansTotal = banList.length / 2;
            ResolvedMassbanEntry[] banEntries = new ResolvedMassbanEntry[_bansTotal];

            if (_bansTotal > 50) {
                e.reply(e.getUser().getAsMention() + ", let's keep the number of bans at once below 50... truncating input...").queue();
                _bansTotal = 50;
            }
            final int bansTotal = _bansTotal;

            boolean cancel = false;
            for (int i = 0; i < bansTotal; i++) {
                String targetUsername = banList[i * 2];
                String banReason = banList[i * 2 + 1];

                if (banReason.isBlank())
                    banReason = null;

                var entry = ResolvedMassbanEntry.resolve(e, targetUsername, banReason, claimedRobloxId);
                if (entry == null)
                    cancel = true;
                banEntries[i] = entry;
            }

            // one or more errors happened during the resolving process
            if (cancel) return;

            final var channel = e.getInteraction().getChannel();
            if (bansTotal > 1)
                channel.sendMessage(e.getUser().getAsMention() + ", mass-banning " + bansTotal + " users...").queue();
            else
                channel.sendMessage(e.getUser().getAsMention() + ", mass-banning " + bansTotal + " user...").queue();

            final AtomicInteger successfulBans = new AtomicInteger(0);
            for (ResolvedMassbanEntry entry : banEntries) {
                BanRequest request = entry.createRequest();
                request.onFulfilled(ignored -> {
                    int currentBans = successfulBans.incrementAndGet();
                    if (currentBans == bansTotal) {
                        channel.sendMessage(e.getUser().getAsMention() + ", finished banning " + bansTotal + " users.").queue();
                    } else {
                        channel.sendMessage(entry.responsibleModerator + ": Banned user " + entry.username + " permanently for reason:\n-# " + entry.reason).queue();
                    }
                });
                request.onNoPermission(() -> channel.sendMessage(e.getUser().getAsMention() + ", you don't have permission to ban users in-game.").queue());
                PendingRequests.add(request);
            }
        } catch (SQLException ex) {
            e.getInteraction().getChannel().sendMessage(e.getUser().getAsMention() + ", something went wrong internally and I couldn't get your linked Roblox ID. Try again in a couple of minutes.").queue();
        }
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {

        if (StaffRoles.blockIfNotStaff(e))
            return;

        long userId = e.getUser().getIdLong();
        Long linkedRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(userId);

        if (linkedRobloxId == null) {
            e.reply("Couldn't get your Roblox ID from your Discord ID. If you haven't already, get your accounts linked with an administrator.").setEphemeral(false).queue();
            return;
        }

        Modal modal = Modal.create("massban_" + linkedRobloxId, "Massban GUI")
                .addActionRow(
                        TextInput.create("bans", "Ban List", TextInputStyle.PARAGRAPH)
                                .setRequired(true)
                                .setPlaceholder("username 1\nreason 1\nusername 2\nreason 2\netc...")
                                .build()
                )
                .build();

        e.replyModal(modal).queue();
    }

    private static class ResolvedMassbanEntry {
        public final long id;
        public final String username;
        public final String reason;
        public final long responsibleModerator;

        private ResolvedMassbanEntry(long id, String username, String reason, long responsibleModerator) {
            this.id = id;
            this.username = username;
            this.reason = reason;
            this.responsibleModerator = responsibleModerator;
        }

        /**
         * Resolve a username into a {@link ResolvedMassbanEntry} and return it.
         * If the username cannot be resolved, a message will be sent in the channel
         * of the {@link ModalInteractionEvent} and {@code null} will be returned.
         *
         * @param username             The username of the user to resolve.
         * @param reason               The reason for the ban.
         * @param responsibleModerator The Roblox ID of the responsible moderator.
         * @return The resolved ban entry, or {@code null} if the username couldn't be resolved.
         */
        public static ResolvedMassbanEntry resolve(ModalInteractionEvent e, String username, String reason, long responsibleModerator) {
            RobloxAPI.User searchResults = RobloxAPI.getUserByCurrentUsername(username);

            if (searchResults == null) {
                e.getInteraction().getChannel().sendMessage(e.getUser().getAsMention() + ", " + getUnknownUsernameDescriptor(username)).queue();
                return null;
            }

            return new ResolvedMassbanEntry(searchResults.userId(), searchResults.username(), reason, responsibleModerator);
        }

        /**
         * Creates a new {@link BanRequest} using the data contained in the current {@link ResolvedMassbanEntry}.
         * The created ban request includes the ID of the user to be banned, the responsible moderator's ID,
         * the reason for the ban, and other fixed parameters like permanent ban status and ban duration.
         *
         * @return A {@link BanRequest} instance representing the ban request to be sent.
         */
        public BanRequest createRequest() {
            return new BanRequest(PendingRequest.getNextRequestId(), this.id, this.responsibleModerator, this.reason, true, 0L, null, null);
        }
    }
}
