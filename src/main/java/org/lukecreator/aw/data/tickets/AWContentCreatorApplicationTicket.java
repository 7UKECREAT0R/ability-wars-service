package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.StaffRoles;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class AWContentCreatorApplicationTicket extends AWTicket {

    public static final long CC_ROLE_ID = 1329637217160925224L;
    public static final int ACCOUNT_LINK_MAX_LENGTH = 512;

    /**
     * The URL to the YouTube/TikTok account applying.
     */
    public String accountLink;

    public AWContentCreatorApplicationTicket(long id, long discordChannelId, long openedTimestamp, boolean isOpen, String closeReason, long closedByDiscordId, JsonObject inputQuestions, long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    @Override
    public void setProperty(@NonNull String key, @Nullable String value, SlashCommandInteractionEvent event) {
        // no properties
    }

    @Override
    public String[] getPropertyChoices() {
        return new String[0];
    }

    @Override
    public Type type() {
        return Type.ContentCreatorApplication;
    }

    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject result = new JsonObject();
        result.addProperty("account-url", this.accountLink);
        return result;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        this.accountLink = json.get("account-url").getAsString();
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
        Label linkInput = Label.of("YouTube Channel/Tiktok Account Link", "Enter the link to the social media channel you are using for your application.",
                TextInput.create("account-link", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("www.youtube.com/@lukecreator")
                        .setRequiredRange(6, ACCOUNT_LINK_MAX_LENGTH)
                        .build());

        Label q1Input = Label.of("Subs/Followers Requirement", "Do you have 1,000+ subscribers/5,000+ followers?",
                StringSelectMenu.create("q1")
                        .setRequiredRange(1, 1)
                        .setRequired(true)
                        .addOption("No", "no")
                        .addOption("Yes", "yes")
                        .build());
        Label q2Input = Label.of("View Requirement", "Do you have 2,500+ views on an Ability Wars long-form YouTube video, or 20,000+ views on a tiktok?",
                StringSelectMenu.create("q2")
                        .setRequiredRange(1, 1)
                        .setRequired(true)
                        .addOption("No", "no")
                        .addOption("Yes", "yes")
                        .build());
        Label q3Input = Label.of("Connection Requirement", "Is your social media page connected to your Discord account via Discord \"Connections\"?",
                StringSelectMenu.create("q3")
                        .setRequiredRange(1, 1)
                        .setRequired(true)
                        .addOption("No", "no")
                        .addOption("Yes", "yes")
                        .build());

        Type type = this.type();
        String customId = type.getCreationModalCustomId(newTicketId);
        return Modal.create(customId, "Application for the Content Creator role").addComponents(linkInput, q1Input, q2Input, q3Input).build();
    }

    @Override
    public TicketAction[] getAvailableActions() {
        return new TicketAction[]{
                new TicketAction("cancel", "Cancel", ButtonStyle.SECONDARY, this.id),
                new TicketAction("deny", "Deny", ButtonStyle.DANGER, this.id),
                new TicketAction("approve", "Approve (gives role automatically)", ButtonStyle.SUCCESS, this.id)
        };
    }

    @Override
    public List<MessageEmbed> getInitialMessageEmbeds(JDA jda) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.RED);
        eb.setTitle("Ticket " + this.id);
        eb.setDescription("Content Creator Application with YouTube/TikTok account.");
        eb.addField("Account Link", this.accountLink, false);
        eb.addField("Requirements", "- 1,000+ Subscribers or 5,000+ Followers\n- 2,500+ Views on Ability Wars long-form video or 20,000+ Views on an Ability Wars TikTok video.\n- Account is connected to their Discord account.", false);
        eb.setFooter("Staff: double check the requirements before accepting!");
        return List.of(eb.build());
    }

    @Override
    public void handleAction(String actionId, ButtonInteractionEvent event) {
        Member clickedMember = event.getMember();

        if (clickedMember == null) {
            event.reply("This button can only be used in Discord servers, not DMs. (how did you even do this???)").setEphemeral(true).queue();
            return;
        }

        long clickedUserId = clickedMember.getIdLong();

        if (actionId.equals("cancel")) {
            // must be ticket opener or staff
            if (this.ownerDiscordId == clickedUserId || StaffRoles.isStaff(event.getMember())) {
                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color.orange)
                        .setTitle("Are you sure you want to cancel this ticket?")
                        .setDescription(clickedMember.getAsMention() + ", please confirm you'd like to cancel this ticket. This action cannot be undone.");
                event.replyEmbeds(eb.build()).addComponents(ActionRow.of(
                        net.dv8tion.jda.api.components.buttons.Button.secondary(AbilityWarsBot.BUTTON_ID_DELETE_PARENT_MESSAGE, "No"),
                        Button.danger(AbilityWarsBot.BUTTON_ID_CANCEL_TICKET_CONFIRM, "Yes")
                )).setEphemeral(true).queue();
                return;
            }
            event.reply("You must be the ticket opener or staff to cancel this ticket.").setEphemeral(true).queue();
            return;
        }

        if (StaffRoles.blockIfNotStaff(event))
            return;

        if (actionId.equals("deny")) {
            try {
                event.deferEdit().queue();
                this.close(event.getJDA(), clickedMember.getUser(), "Sorry! Your Content Creator role application has been denied.", null, null);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if (actionId.equals("approve")) {
            final MessageEmbed failureEmbed = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("NOTE: not so automagic")
                    .setDescription("I couldn't give you the role automatically, so please message a staff to have them give it to you!")
                    .build();

            Guild guild = clickedMember.getGuild();
            Role ccRole = guild.getRoleById(CC_ROLE_ID);
            if (ccRole == null) {
                event.reply("I literally cannot find the Content Creator role, ID %d.".formatted(CC_ROLE_ID)).setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            guild.addRoleToMember(clickedMember, ccRole).queue(success -> {
                try {
                    this.close(event.getJDA(), clickedMember.getUser(), "Your Content Creator application has been approved! Enjoy your role!", null, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, failure -> {
                try {
                    this.close(event.getJDA(), clickedMember.getUser(), "Your Content Creator application has been approved, but...", null, failureEmbed);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return;
        }
    }

    /**
     * Returns {@code true} if the user picked "No" in the given modal, {@code false} otherwise.
     *
     * @param mapping The mapping to check.
     */
    private boolean pickedNo(ModalMapping mapping) {
        List<String> selections = mapping.getAsStringList();
        return selections.size() == 1 && selections.get(0).equals("no");
    }

    @Override
    public void loadFromModalResponse(ModalInteractionEvent event, Consumer<Boolean> onFinishedLoading) {
        ModalMapping linkMapping = event.getValue("account-link");
        ModalMapping q1Mapping = event.getValue("q1");
        ModalMapping q2Mapping = event.getValue("q2");
        ModalMapping q3Mapping = event.getValue("q3");

        if (linkMapping == null || q1Mapping == null || q2Mapping == null || q3Mapping == null) {
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            onFinishedLoading.accept(false);
            return;
        }

        String linkInput = linkMapping.getAsString();
        if (linkInput.isBlank() || linkInput.length() > ACCOUNT_LINK_MAX_LENGTH || !EmbedBuilder.URL_PATTERN.matcher(linkInput).matches()) {
            event.getHook().editOriginal("Please input a valid URL to your YouTube or TikTok channel.").queue();
            onFinishedLoading.accept(false);
            return;
        }

        // if they answer "no" to any of the questions, don't open the ticket and let them know
        // that they don't yet meet the requirements.
        if (this.pickedNo(q1Mapping) || this.pickedNo(q2Mapping) || this.pickedNo(q3Mapping)) {
            event.getHook().editOriginal("You don't yet meet the requirements to apply for the Content Creator role! You can always apply again once you've met the requirements!").queue();
            onFinishedLoading.accept(false);
            return;
        }

        onFinishedLoading.accept(true);
        return;
    }
}
