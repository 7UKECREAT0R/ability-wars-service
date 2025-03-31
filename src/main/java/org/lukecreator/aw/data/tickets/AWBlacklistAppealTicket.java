package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.data.AWTicket;

import java.sql.SQLException;
import java.util.List;

public class AWBlacklistAppealTicket extends AWTicket {
    public AWBlacklistAppealTicket(long id,
                                   long discordChannelId, long openedTimestamp,
                                   boolean isOpen, String closeReason,
                                   long closedByDiscordId, JsonObject inputQuestions,
                                   long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen,
                closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    @Override
    public void setProperty(@NotNull String key, @Nullable String value, SlashCommandInteractionEvent event) throws SQLException {
        event.reply("This ticket doesn't have any supported properties that can be changed.").setEphemeral(true).queue();
    }

    @Override
    public String[] getPropertyChoices() {
        return new String[0];
    }

    @Override
    public Type type() {
        return Type.BlacklistAppeal;
    }

    @Override
    public JsonObject getInputQuestionsJSON() {
        return null;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {

    }

    @Override
    public void afterCacheLoaded() {

    }

    @Override
    public void afterTicketChannelCreated(TextChannel channel) {

    }

    @Override
    public void afterInitialMessageSent(TextChannel channel, Message message) {

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

    }

    @Override
    public Modal createInputModal(long newTicketId) {
        // TODO
        return null;
    }

    @Override
    public TicketAction[] getAvailableActions() {
        return new TicketAction[0];
    }

    @Override
    public List<MessageEmbed> getInitialMessageEmbeds(JDA jda) {
        return null;
    }

    @Override
    public void handleAction(String actionId, ButtonInteractionEvent event) {

    }

    @Override
    public boolean loadFromModalResponse(ModalInteractionEvent event) throws SQLException {
        return false;
    }

}
