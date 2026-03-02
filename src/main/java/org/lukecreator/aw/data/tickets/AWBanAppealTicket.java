package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.function.Consumer;

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
        return modal.addComponents(
                Label.of("Why were you banned?", TextInput.create("question-a", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(10)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder(this.isForDiscord ?
                                "Tell us why you were banned from our Discord." :
                                "Tell us why you were banned from Ability Wars."
                        )
                        .build()),
                Label.of("Why should you be unbanned?", TextInput.create("question-b", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(10)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder("We're taking a risk by unbanning you. Why should we?")
                        .build()),
                Label.of("Anything else you'd like to tell us?", TextInput.create("question-c", TextInputStyle.PARAGRAPH)
                        .setRequired(false)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build())
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
    public void loadFromModalResponse(ModalInteractionEvent event, Consumer<Boolean> onFinishedLoading) throws SQLException {
        super.loadFromModalResponse(event, superResult -> {
            if (!superResult) {
                onFinishedLoading.accept(false);
                return;
            }

            // the super method already called `deferReply` with `ephemeral` true
            ModalMapping questionAMapping = event.getInteraction().getValue("question-a");
            ModalMapping questionBMapping = event.getInteraction().getValue("question-b");
            ModalMapping questionCMapping = event.getInteraction().getValue("question-c");

            if (questionAMapping == null || questionBMapping == null) {
                // editing since the super.loadFromModalResponse call already sent a reply.
                event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
                onFinishedLoading.accept(false);
                return;
            }

            this.questionA = questionAMapping.getAsString();
            this.questionB = questionBMapping.getAsString();
            this.questionC = questionCMapping == null ? null : questionCMapping.getAsString();

            if (!this.isForDiscord && this.robloxUserToUnban != null) {
                if (isReasonBecauseOfIPBan(this.questionA) || isReasonBecauseOfIPBan(this.questionB)) {
                    String username = this.robloxUserToUnban.username();
                    event.getHook().editOriginalEmbeds(getResponseForIPBan(username)).queue();
                    onFinishedLoading.accept(false);
                    return;
                }
            }

            if (this.questionA.isBlank() || this.questionB.isBlank()) {
                event.getHook().editOriginal("You cannot leave questions 1 or 2 blank. How did you even do this?").queue();
                onFinishedLoading.accept(false);
                return;
            }
            if (this.questionC != null && this.questionC.isBlank())
                this.questionC = null; // don't bother dealing with a blank string. it's either null or not.

            onFinishedLoading.accept(true);
            return;
        });
    }
}
