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
import org.lukecreator.aw.data.AWBan;

import java.sql.SQLException;
import java.util.function.Consumer;

public class AWBanDisputeTicket extends AWUnbanTicket {
    /**
     * "What were you false-banned for?"
     */
    private String questionA;
    /**
     * "What do you think happened?"
     */
    private String questionB;
    /**
     * "Agreement"
     */
    private String questionC;

    public AWBanDisputeTicket(long id, long discordChannelId, long openedTimestamp, boolean isOpen, String closeReason, long closedByDiscordId, JsonObject inputQuestions, long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    @Override
    public Type type() {
        return Type.BanDispute;
    }

    @Override
    protected boolean isAppeal() {
        return false;
    }


    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject base = super.getInputQuestionsJSON();
        base.addProperty("question_a", this.questionA);
        base.addProperty("question_b", this.questionB);
        base.addProperty("question_c", this.questionC);
        return base;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        super.processInputQuestionsJSON(json);
        this.questionA = json.get("question_a").getAsString();
        this.questionB = json.get("question_b").getAsString();
        this.questionC = json.get("question_c").getAsString();
    }

    @Override
    public void afterTicketChannelCreated(TextChannel channel) {
        // the old "consent" form we used
/*        if (!this.isForDiscord) {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Video Evidence Consent Form")
                    .setDescription("We have video evidence related to the incident that led to your ban. Do you consent to our team reviewing this footage as part of evaluating your dispute?")
                    .setColor(Color.orange)
                    .build();
            channel.sendMessageEmbeds(embed)
                    .addComponents(
                            ActionRow.of(
                                    Button.of(ButtonStyle.SUCCESS, "evidenceagree", "I Agree"),
                                    Button.of(ButtonStyle.DANGER, "evidencedisagree", "I Disagree")
                            )
                    ).mention(UserSnowflake.fromId(this.ownerDiscordId)).queue();
        }*/
    }


    @Override
    public Modal.Builder finishInputModal(Modal.Builder modal) {
        return modal.addComponents(
                Label.of("What were you false-banned for?", "Don't speculate. Tell us why you were banned.", TextInput.create("question-a", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build()),
                Label.of("What do you think happened?", "Explain exactly what you think happened, in detail.", TextInput.create("question-b", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build()),
                Label.of("Agreement", "Please swear that your answers are the full truth.", TextInput.create("question-c", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .build())
        );
    }

    @Override
    protected EmbedBuilder finishInitialMessageEmbed(EmbedBuilder eb) {
        eb.addField("What were you false-banned for?", this.questionA, false);
        eb.addField("What do you think happened?", this.questionB, false);
        eb.addField("Agreement (do you swear that your answers are the truth?)", this.questionC, false);

        if (this.temporaryEvidence != null && this.temporaryEvidence.length > 0) {
            String plural = this.temporaryEvidence.length == 1 ? "piece of evidence" : "pieces of evidence";
            eb.addField("Evidence", "⚠️ Found " + this.temporaryEvidence.length + " " + plural + " related to this ban.", false);
        }

        return eb;
    }

    @Override
    public void loadFromModalResponse(ModalInteractionEvent event, Consumer<Boolean> onFinishedLoading) throws SQLException {
        super.loadFromModalResponse(event, superResult -> {
            if (!superResult) {
                onFinishedLoading.accept(false);
                return;
            }

            if (!this.isForDiscord) {
                if (this.temporaryInfoFulfillment == null) {
                    onFinishedLoading.accept(false); // shouldn't happen, but if it does, the message has already been edited.
                    return;
                }

                AWBan currentBan = this.temporaryInfoFulfillment.getMostRecentBan();
                if (currentBan != null) {
                    if (isReasonBecauseOfAnticheatBan(currentBan.reason())) {
                        event.getHook().editOriginal("This ban cannot be disputed; the decision is final.").queue();
                        onFinishedLoading.accept(false);
                        return;
                    }

                    // get evidence stubs
                    try {
                        this.temporaryEvidence = currentBan.getLinkedEvidence();
                    } catch (SQLException e) {
                        event.getHook().editOriginal("Something went wrong while fetching evidence for the ban. Please report to the devs if this keeps happening:\n```\n" + e + "\n```").queue();
                        onFinishedLoading.accept(false);
                        return;
                    }
                }
            }

            // the super method already called `deferReply` with `ephemeral` true
            ModalMapping questionAMapping = event.getInteraction().getValue("question-a");
            ModalMapping questionBMapping = event.getInteraction().getValue("question-b");
            ModalMapping questionCMapping = event.getInteraction().getValue("question-c");

            if (questionAMapping == null || questionBMapping == null || questionCMapping == null) {
                // editing since the super.loadFromModalResponse call already sent a reply.
                event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
                onFinishedLoading.accept(false);
                return;
            }

            this.questionA = questionAMapping.getAsString();
            this.questionB = questionBMapping.getAsString();
            this.questionC = questionCMapping.getAsString();

            if (!this.isForDiscord && this.robloxUserToUnban != null) {
                if (isReasonBecauseOfIPBan(this.questionA) || isReasonBecauseOfIPBan(this.questionB)) {
                    String username = this.robloxUserToUnban.username();
                    event.getHook().editOriginalEmbeds(getResponseForIPBan(username)).queue();
                    onFinishedLoading.accept(false);
                    return;
                }
            }

            if (this.questionA.isBlank() || this.questionB.isBlank() || this.questionC.isBlank()) {
                event.getHook().editOriginal("You cannot leave any of the questions blank. How did you even do this?").queue();
                onFinishedLoading.accept(false);
                return;
            }

            onFinishedLoading.accept(true);
            return;
        });
    }

}
