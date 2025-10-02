package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.sql.SQLException;

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
        if (!this.isForDiscord) {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Video Evidence Consent Form")
                    .setDescription("We have video evidence related to the incident that led to your ban. Do you consent to our team reviewing this footage as part of evaluating your dispute?")
                    .setColor(Color.orange)
                    .build();
            channel.sendMessageEmbeds(embed)
                    .addActionRow(
                            Button.of(ButtonStyle.SUCCESS, "evidenceagree", "I Agree"),
                            Button.of(ButtonStyle.DANGER, "evidencedisagree", "I Disagree")
                    ).mention(UserSnowflake.fromId(this.ownerDiscordId)).queue();
        }
    }


    @Override
    public Modal.Builder finishInputModal(Modal.Builder modal) {
        return modal.addActionRow(
                TextInput.create("question-a", "What were you false-banned for?", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder(this.isForDiscord ?
                                "Don't speculate. Tell us why you were banned from our Discord." :
                                "Don't speculate. Tell us why you were banned from Ability Wars."
                        )
                        .build()
        ).addActionRow(
                TextInput.create("question-b", "What do you think happened?", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder("Explain exactly what you think happened, in detail.")
                        .build()
        ).addActionRow(
                TextInput.create("question-c", "Agreement", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setMinLength(2)
                        .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                        .setPlaceholder("Please swear that your answers are the full truth.")
                        .build()
        );
    }

    @Override
    protected EmbedBuilder finishInitialMessageEmbed(EmbedBuilder eb) {
        eb.addField("What were you false-banned for?", this.questionA, false);
        eb.addField("What do you think happened?", this.questionB, false);
        eb.addField("Agreement (do you swear that your answers are the truth?)", this.questionC, false);
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

        if (questionAMapping == null || questionBMapping == null || questionCMapping == null) {
            // editing since the super.loadFromModalResponse call already sent a reply.
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            return false;
        }

        this.questionA = questionAMapping.getAsString();
        this.questionB = questionBMapping.getAsString();
        this.questionC = questionCMapping.getAsString();

        if (!this.isForDiscord && this.robloxUserToUnban != null) {
            if (isReasonBecauseOfIPBan(this.questionA) || isReasonBecauseOfIPBan(this.questionB)) {
                String username = this.robloxUserToUnban.username();
                event.getHook().editOriginalEmbeds(getResponseForIPBan(username)).queue();
                return false;
            }
        }

        if (this.questionA.isBlank() || this.questionB.isBlank() || this.questionC.isBlank()) {
            event.getHook().editOriginal("You cannot leave any of the questions blank. How did you even do this?").queue();
            return false;
        }

        return true;
    }

}
