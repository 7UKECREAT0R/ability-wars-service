package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public class AWBanAppealTicket extends AWUnbanTicket {
    /**
     * "Why were you banned?"
     */
    private String questionA;
    /**
     * "Why should you be unbanned?"
     */
    private String questionB;
    /**
     * "Anything else you'd like to tell us?" (can be null)
     */
    @Nullable
    private String questionC;

    public AWBanAppealTicket(long id, long discordChannelId, long openedTimestamp, boolean isOpen, String closeReason, long closedByDiscordId, JsonObject inputQuestions, long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    @Override
    public Type type() {
        return Type.BanAppeal;
    }

    @Override
    protected boolean isAppeal() {
        return true;
    }


    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject base = super.getInputQuestionsJSON();
        base.addProperty("question_a", this.questionA);
        base.addProperty("question_b", this.questionB);
        if (this.questionC != null)
            base.addProperty("question_c", this.questionC);
        return base;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        super.processInputQuestionsJSON(json);
        this.questionA = json.get("question_a").getAsString();
        this.questionB = json.get("question_b").getAsString();
        this.questionC = json.has("question_c") ? json.get("question_c").getAsString() : null;
    }

    @Override
    public void afterTicketChannelCreated(TextChannel channel) {

    }


    @Override
    public Modal.Builder finishInputModal(Modal.Builder modal) {
        return modal.addActionRow(
                TextInput.create("question-a", "Why were you banned?", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder(this.isForDiscord ?
                                "Tell us why you were banned from our Discord." :
                                "Tell us why you were banned from Ability Wars."
                        )
                        .build()
        ).addActionRow(
                TextInput.create("question-b", "Why should you be unbanned?", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder("We're taking a risk by unbanning you. Why should we?")
                        .build()
        ).addActionRow(
                TextInput.create("question-c", "Anything else you'd like to tell us?", TextInputStyle.PARAGRAPH)
                        .setRequired(false)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build()
        );
    }

    @Override
    protected EmbedBuilder finishInitialMessageEmbed(EmbedBuilder eb) {
        eb.addField("Why were you banned?", this.questionA, false);
        eb.addField("Why should you be unbanned?", this.questionB, false);
        eb.addField("Anything else you'd like to tell us?", this.questionC == null ? "-# (empty)" : this.questionC, false);
        return eb;
    }

    @Override
    public boolean loadFromModalResponse(ModalInteractionEvent event) throws SQLException {
        if (!super.loadFromModalResponse(event))
            return false;

        // the super method already called `deferReply` with `ephemeral` true
        ModalMapping questionAMapping = event.getInteraction().getValue("question-a");
        ModalMapping questionBMapping = event.getInteraction().getValue("question-b");
        ModalMapping questionCMapping = event.getInteraction().getValue("question-c");

        if (questionAMapping == null || questionBMapping == null) {
            // editing since the super.loadFromModalResponse call already sent a reply.
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            return false;
        }

        this.questionA = questionAMapping.getAsString();
        this.questionB = questionBMapping.getAsString();
        this.questionC = questionCMapping == null ? null : questionCMapping.getAsString();

        if (this.questionA.isBlank() || this.questionB.isBlank()) {
            event.getHook().editOriginal("You cannot leave questions 1 or 2 blank. How did you even do this?").queue();
            return false;
        }
        if (this.questionC != null && this.questionC.isBlank())
            this.questionC = null; // don't bother dealing with a blank string. it's either null or not.

        return true;
    }
}
