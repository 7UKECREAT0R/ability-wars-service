package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.ActionModals;
import org.lukecreator.aw.discord.StaffRoles;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class AWDataLossTicket extends AWTicket {

    public static final int PURCHASED_PRODUCT_NAME_MAX_LENGTH = 100;

    /**
     * The Roblox User ID of the gift purchaser.
     */
    public long purchaserId;
    /**
     * The Roblox username of the gift purchaser.
     */
    public String purchaserUsername;
    /**
     * The Roblox User of the gift purchaser. May be null if this ticket is recalled after being completed.
     */
    @Nullable
    public RobloxAPI.User purchaser;

    /**
     * The Roblox User ID of the gift recipient.
     */
    public long recipientId;
    /**
     * The Roblox username of the gift recipient.
     */
    public String recipientUsername;
    /**
     * The Roblox User of the gift recipient. May be null if this ticket is recalled after being completed.
     */
    @Nullable
    public RobloxAPI.User recipient;

    /**
     * The name of the product that was purchased/gifted. Max length: {@link #PURCHASED_PRODUCT_NAME_MAX_LENGTH}
     * This is a user-entered field, so it may (and likely will) be wrong.
     */
    public String purchasedProductName;

    public AWDataLossTicket(long id, long discordChannelId, long openedTimestamp, boolean isOpen, String closeReason, long closedByDiscordId, JsonObject inputQuestions, long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }


    @Override
    public void setProperty(@NotNull String key, @Nullable String value, SlashCommandInteractionEvent event) throws SQLException {
        if (key.equalsIgnoreCase("product-name")) {
            if (value == null || value.isBlank()) {
                event.reply("Product name cannot be empty. Please provide a valid product name.").setEphemeral(true).queue();
                return;
            }
            if (value.length() > PURCHASED_PRODUCT_NAME_MAX_LENGTH) {
                event.reply("Product name cannot be longer than %d characters. Please provide a shorter product name.".formatted(PURCHASED_PRODUCT_NAME_MAX_LENGTH)).setEphemeral(true).queue();
                return;
            }
            this.purchasedProductName = value;
            this.updateInDatabase();
        }
    }

    @Override
    public String[] getPropertyChoices() {
        return new String[]{"product-name"};
    }

    @Override
    public Type type() {
        return Type.DataLossReport;
    }

    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject result = new JsonObject();
        result.addProperty("purchaser", this.purchaserId);
        result.addProperty("recipient", this.recipientId);
        result.addProperty("product-name", this.purchasedProductName);
        return result;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        this.purchaserId = json.get("purchaser").getAsLong();
        this.recipientId = json.get("recipient").getAsLong();
        this.purchasedProductName = json.get("product-name").getAsString();
    }

    @Override
    public void afterCacheLoaded() {
        if (this.purchaser == null) {
            this.purchaser = RobloxAPI.getUserById(this.purchaserId);
            if (this.purchaser != null)
                this.purchaserUsername = this.purchaser.username();
        }
        if (this.recipient == null) {
            this.recipient = RobloxAPI.getUserById(this.recipientId);
            if (this.recipient != null)
                this.recipientUsername = this.recipient.username();
        }
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
        Label purchaserInput = Label.of("Purchaser", "The username or ID of the user who purchased the gift with Robux.",
                TextInput.create("purchaser", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setRequiredRange(2, 20)
                        .build());
        Label recipientInput = Label.of("Recipient", "The username or ID of the user who didn't receive the gift.",
                TextInput.create("recipient", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setRequiredRange(2, 20)
                        .build());
        Label productNameInput = Label.of("Purchased Product", "The name of the product that was purchased.",
                TextInput.create("product-name", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("God Punch, 2x Punches, etc.")
                        .build());

        Type type = this.type();
        String customId = type.getCreationModalCustomId(newTicketId);
        return Modal.create(customId, type.description)
                .addComponents(purchaserInput, recipientInput, productNameInput)
                .build();
    }

    @Override
    public TicketAction[] getAvailableActions() {
        return new TicketAction[]{
                new TicketAction("cancel", "Cancel", ButtonStyle.SECONDARY, this.id),
                new TicketAction("close", "Resolve (nothing could be done)", ButtonStyle.DANGER, this.id),
                new TicketAction("close-good", "Resolve (restored)", ButtonStyle.SUCCESS, this.id)
        };
    }

    @Override
    public List<MessageEmbed> getInitialMessageEmbeds(JDA jda) {
        User ticketOwner = jda.getUserById(this.ownerDiscordId);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Ticket (" + this.id + ")");
        eb.setColor(new Color(194, 105, 54));

        if (ticketOwner == null)
            eb.setDescription("Data/Gift loss ticket, opened by <@" + this.ownerDiscordId + ">");
        else
            eb.setDescription("Data/Gift loss ticket, opened by %s (%s)".formatted(ticketOwner.getAsMention(), ticketOwner.getName()));

        if (this.purchaser != null) {
            eb.addField("Gift Purchaser", "`%s`, [Roblox Profile](%s)".formatted(
                    this.purchaserUsername,
                    this.purchaser.getProfileURL()
            ), false);
        } else {
            eb.addField("Gift Purchaser", "Roblox ID: `%d`".formatted(this.purchaserId), false);
        }

        if (this.recipient != null) {
            eb.addField("Gift Recipient", "`%s`, [Roblox Profile](%s)".formatted(
                    this.recipientUsername,
                    this.recipient.getProfileURL()
            ), true);
        } else {
            eb.addField("Gift Recipient", "Roblox ID: `%d`".formatted(this.recipientId), true);
        }

        eb.addField("Purchased Product Name", this.purchasedProductName, false);

        return List.of(eb.build());
    }

    @Override
    public void handleAction(String actionId, ButtonInteractionEvent event) {
        User clickedUser = event.getUser();
        long clickedUserId = clickedUser.getIdLong();

        if (actionId.equals("cancel")) {
            // must be ticket opener or staff
            if (this.ownerDiscordId == clickedUserId || StaffRoles.isStaff(event.getMember())) {
                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color.orange)
                        .setTitle("Are you sure you want to cancel this ticket?")
                        .setDescription(clickedUser.getAsMention() + ", please confirm you'd like to cancel this ticket. This action cannot be undone.");
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

        if (actionId.equals("close")) {
            Modal modal = ActionModals.closeTicketWithCustomReason(this);
            event.replyModal(modal).queue();
            return;
        }
        if (actionId.equals("close-good")) {
            try {
                event.deferEdit().queue();
                this.close(event.getJDA(), clickedUser, "Your gift/purchase has been restored, thanks for letting us help!", null, null);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loadFromModalResponse(ModalInteractionEvent event, Consumer<Boolean> onFinishedLoading) throws SQLException {
        ModalMapping purchaserMapping = event.getValue("purchaser");
        ModalMapping recipientMapping = event.getValue("recipient");
        ModalMapping productNameMapping = event.getValue("product-name");

        if (purchaserMapping == null || recipientMapping == null || productNameMapping == null) {
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            onFinishedLoading.accept(false);
            return;
        }

        String purchaserInput = purchaserMapping.getAsString();
        String recipientInput = recipientMapping.getAsString();
        String productNameInput = productNameMapping.getAsString();

        if (purchaserInput.isBlank()) {
            event.getHook().editOriginal("Purchaser cannot be empty. Please provide a valid username or ID.").queue();
            onFinishedLoading.accept(false);
            return;
        }
        if (recipientInput.isBlank()) {
            event.getHook().editOriginal("Recipient cannot be empty. Please provide a valid username or ID.").queue();
            onFinishedLoading.accept(false);
            return;
        }
        if (productNameInput.isBlank()) {
            event.getHook().editOriginal("Product name cannot be empty. Please provide a valid product name.").queue();
            onFinishedLoading.accept(false);
            return;
        }
        if (productNameInput.length() > PURCHASED_PRODUCT_NAME_MAX_LENGTH) {
            event.getHook().editOriginal("Please input a valid product name. It cannot be longer than %d characters.".formatted(PURCHASED_PRODUCT_NAME_MAX_LENGTH)).queue();
            onFinishedLoading.accept(false);
            return;
        }

        this.purchaser = RobloxAPI.getUserByInput(purchaserInput, true);
        if (this.purchaser == null) {
            event.getHook().editOriginal("Couldn't find the Roblox user with the username/ID `%s`. Please double-check your responses!".formatted(purchaserInput)).queue();
            onFinishedLoading.accept(false);
            return;
        }

        this.recipient = RobloxAPI.getUserByInput(recipientInput, true);
        if (this.recipient == null) {
            event.getHook().editOriginal("Couldn't find the Roblox user with the username/ID `%s`. Please double-check your responses!".formatted(recipientInput)).queue();
            onFinishedLoading.accept(false);
            return;
        }

        this.purchaserId = this.purchaser.userId();
        this.purchaserUsername = this.purchaser.username();
        this.recipientId = this.recipient.userId();
        this.recipientUsername = this.recipient.username();
        this.purchasedProductName = productNameInput;
        onFinishedLoading.accept(true);
        return;
    }
}
