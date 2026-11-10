package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.BloxlinkAPI;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.fulfillments.InfoFulfillment;
import org.lukecreator.aw.webserver.requests.InfoRequest;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PunchRolesCommand extends BotCommand {
    private static final long VERIFY_CHANNEL_ID = 1329631643430031401L;
    private static final Map<Long, Long> PUNCH_ROLES = Map.of(
            100_000L, 1498420252172750908L,
            250_000L, 1498421210222952538L,
            500_000L, 1498421597344632962L,
            750_000L, 1498428218590494862L,
            1_000_000L, 1498427847402848336L,
            2_500_000L, 1498422995444699299L
    );

    public PunchRolesCommand() {
        super("get-punch-roles", "Gives you your roles based on how many punches you have in-game!");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        final Guild guild = e.getGuild();
        final Member member = e.getMember();

        if (member == null || guild == null || guild.getIdLong() != AbilityWarsBot.AW_GUILD_ID) {
            e.reply("This command can only be used in the Ability Wars server.").queue();
            return;
        }

        e.deferReply(false).queue();

        long discordId = e.getUser().getIdLong();
        Long robloxId = BloxlinkAPI.lookupRobloxId(discordId);

        if (robloxId == null) {
            e.getHook().editOriginal("You haven't linked your account on Bloxlink yet! Head over to <#" + VERIFY_CHANNEL_ID + ">!").queue();
            return;
        }

        PendingRequest infoRequest = new InfoRequest(PendingRequest.getNextRequestId(), robloxId)
                .onFulfilled(fulfillment -> {
                    InfoFulfillment info = (InfoFulfillment) fulfillment;
                    long punches = info.punches;
                    List<Role> roles = new ArrayList<>();
                    for (Map.Entry<Long, Long> entry : PUNCH_ROLES.entrySet()) {
                        if (punches >= entry.getKey()) {
                            Role roleById = guild.getRoleById(entry.getValue());
                            if (roleById == null) {
                                System.out.println("get-punch-roles: Role not found by ID: " + entry.getValue());
                                continue;
                            }
                            roles.add(roleById);
                        }
                    }

                    if (roles.isEmpty()) {
                        e.getHook().editOriginalEmbeds(new EmbedBuilder().setDescription("-# You don't have enough punches yet to qualify for a role!").build()).queue();
                        return;
                    }

                    // give them the roles with one request
                    guild.modifyMemberRoles(member, roles, Collections.emptySet()).queue();

                    // show the roles they got
                    List<String> roleStrings = roles.stream().map(r -> "- " + r.getAsMention()).toList();
                    String roleList = String.join("\n", roleStrings);
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Punch Roles")
                            .setDescription("-# You've received the following roles!:\n\n" + roleList)
                            .setColor(Color.orange);
                    e.getHook().editOriginalEmbeds(eb.build()).setAllowedMentions(Collections.emptyList()).queue();
                });

        PendingRequests.add(infoRequest);
    }
}
