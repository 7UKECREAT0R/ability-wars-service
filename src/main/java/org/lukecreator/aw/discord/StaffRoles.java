package org.lukecreator.aw.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.lukecreator.aw.data.DiscordRobloxLinks;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class StaffRoles {
    public static final long[] staffRoles = new long[]{
            // ability wars
            991532797175021661L,    // oner
            934491303352360981L,    // co-oner,
            1329664483999612970L,   // the goon squad
            1329636771654275125L,   // Staff Manager
            1329640373991248003L,   // Administrator
            1329628247595683860L,   // Senior Moderator
            1329628248279093281L,   // Moderator

            // ability ward,
            978531510711910411L,    // Luke
            1017351941174595586L,   // President of the United States
            983898536439611513L,    // jiggle my nutsack around a lil bit
            978531518798495785L,    // staff

            // prego (for testing)
            878308106138972200L,    // *,
            987494095880597565L     // pregoonians
    };

    public static boolean isStaffRole(long id) {
        for (long staffRole : staffRoles) {
            if (staffRole == id) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStaffRole(Role role) {
        return isStaffRole(role.getIdLong());
    }

    /**
     * Blocks the slash command interaction if the executing member does not have a staff role or a manually linked account to Roblox.
     *
     * @param e The slash command interaction event to possibly block.
     * @return True if the interaction was blocked, false if nothing happened.
     */
    public static boolean blockIfNotStaff(SlashCommandInteractionEvent e) {
        Member member = e.getMember();

        if (isStaff(member))
            return false;

        // block interaction
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Staff Only Beyond This Point")
                .setDescription("To use this command/interaction, you must be a staff member for Ability Wars and have a manually linked Roblox account. (contact an administrator if you're staff but don't have one yet)")
                .setFooter("tldr; if you're not staff, this isn't a command for you!")
                .setColor(Color.RED);

        if (e.getInteraction().getHook().isExpired()) {
            MessageChannel channel = e.getChannel();
            channel.sendMessageEmbeds(eb.build()).queue();
        } else {
            if (e.getInteraction().isAcknowledged()) {
                e.getInteraction().getHook().editOriginalEmbeds(eb.build()).queue();
            } else {
                e.replyEmbeds(eb.build()).setEphemeral(false).queue();
            }
        }
        return true;
    }

    /**
     * Blocks the button interaction if the executing member does not have a staff role or a manually linked account to Roblox.
     *
     * @param e The button interaction event to possibly block.
     * @return True if the interaction was blocked, false if nothing happened.
     */
    public static boolean blockIfNotStaff(ButtonInteractionEvent e) {
        Member member = e.getMember();

        if (isStaff(member))
            return false;

        // block interaction
        User user = e.getUser();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Staff Only Beyond This Point")
                .setDescription(user.getAsMention() + ", to use this button, you must be a staff member for Ability Wars and have a manually linked Roblox account. (contact an administrator if you're staff but don't have one yet)")
                .setFooter("tldr; if you're not staff, this isn't a button for you!")
                .setColor(Color.RED);

        if (e.getInteraction().getHook().isExpired()) {
            MessageChannel channel = e.getChannel();
            channel.sendMessageEmbeds(eb.build()).queue(message -> {
                message.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
            });
        } else {
            if (e.getInteraction().isAcknowledged()) {
                e.getInteraction().getHook()
                        .editOriginalEmbeds(eb.build())
                        .setContent(null)
                        .queue();
            } else {
                e.replyEmbeds(eb.build()).setEphemeral(false).queue(hook -> {
                    hook.deleteOriginal().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
                });
            }
        }
        return true;
    }

    /**
     * Determines whether a given member is considered staff. A member is classified as staff if they have
     * at least one "staff role" and a valid manually linked Roblox account associated with their Discord ID.
     *
     * @param member The Discord member to check for staff classification. If null, the method will return false.
     * @return True if the member has a staff role and a valid linked Roblox account, false otherwise.
     */
    public static boolean isStaff(Member member) {
        if (member == null)
            return false;

        if (member.getRoles().stream().anyMatch(StaffRoles::isStaffRole)) {
            // has a staff role, now check for a manually linked account.
            long discordId = member.getIdLong();
            try {
                Long robloxId = DiscordRobloxLinks.robloxIdFromDiscordId(discordId);
                if (robloxId != null && robloxId != 0L) {
                    return true;
                }
            } catch (SQLException ignored) {
                return false;
            }
        }

        return false;
    }

    /**
     * Checks whether the specified member has at least one role that is classified as a staff role.
     *
     * @param mentionedMember The Discord member whose roles are to be checked.
     * @return True if the member has at least one staff role, false otherwise.
     */
    public static boolean hasStaffRole(Member mentionedMember) {
        List<Role> roles = mentionedMember.getRoles();
        return roles.stream().anyMatch(StaffRoles::isStaffRole);
    }
}
