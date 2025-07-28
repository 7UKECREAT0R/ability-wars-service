package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.BloxlinkAPI;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWBan;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.fulfillments.InfoFulfillment;
import org.lukecreator.aw.webserver.requests.InfoRequest;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

public class StatsCommand extends BotCommand {
    public StatsCommand() {
        super("aw-stats", "Get stats about someone in Ability Wars.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to check.", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        RobloxAPI.User user;

        // start "thinking" as the bot, since the request could take multiple seconds
        e.deferReply().queue();

        if (e.getOption("target") == null) {
            long discordId = e.getUser().getIdLong();
            Long attemptedRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(discordId);
            if (attemptedRobloxId == null) {
                // try bloxlink API
                attemptedRobloxId = BloxlinkAPI.lookupRobloxId(discordId);
                if (attemptedRobloxId == null) {
                    e.getInteraction().getHook().editOriginal("Couldn't figure out your Roblox account! Try manually putting your username into the command.").queue();
                    return;
                }
            }
            user = RobloxAPI.getUserById(attemptedRobloxId);
            if (user == null) {
                e.getInteraction().getHook().editOriginal("Couldn't figure out your Roblox account! Try manually putting your username into the command.").queue();
                return;
            }
        } else {
            var usernameOption = e.getOption("target");
            if (usernameOption == null)
                return;
            String username = usernameOption.getAsString();
            user = RobloxAPI.getUserByInput(username, true);

            if (user == null) {
                e.getInteraction().getHook().editOriginal(getUnknownUsernameDescriptor(username)).queue();
                return;
            }
        }

        long robloxId = user.userId();

        // make a request for user information
        PendingRequest request = new InfoRequest(PendingRequest.getNextRequestId(), robloxId)
                .onFulfilled((fulfillment -> {
                    InfoFulfillment info = (InfoFulfillment) fulfillment;
                    boolean isBanned = false;
                    if (info.bans != null && info.bans.length > 0) {
                        // sort bans by "started" descending
                        Arrays.sort(info.bans, (a, b) -> Long.compare(b.starts(), a.starts()));
                        // get most recent ban
                        AWBan mostRecentBan = info.bans[0];
                        if (mostRecentBan.ends() == null) {
                            isBanned = true;
                        } else {
                            long now = System.currentTimeMillis();
                            long mostRecentBanEnds = mostRecentBan.ends();
                            isBanned = mostRecentBanEnds > now;
                        }
                    }
                    e.getInteraction().getHook().editOriginalEmbeds(
                            new EmbedBuilder()
                                    .setTitle("Stats for " + user.username(), user.getProfileURL())
                                    .setDescription("Requested by " + e.getUser().getAsMention())
                                    .setColor(Color.CYAN)
                                    .addField("Punches", String.format("%,d", info.punches), true)
                                    .addField("Gamepasses", String.join(", ", info.gamepassNames()), true)
                                    .addField("Is Banned?", (isBanned) ? "Yes (see `/aw-ban-status`)" : "No", true)
                                    .build()
                    ).queue();
                }));
        PendingRequests.add(request);
    }
}
