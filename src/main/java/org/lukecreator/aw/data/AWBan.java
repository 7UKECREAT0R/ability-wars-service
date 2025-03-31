package org.lukecreator.aw.data;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * A record of a ban in Ability Wars.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public final class AWBan {
    private final long userId;
    private final Long responsibleModerator;
    private final String reason;
    private final boolean isLegacy;
    private final long starts;
    private final Long ends;
    public Long linkedTicketId;

    /**
     * @param userId               The ID of the user who was banned.
     * @param responsibleModerator The Roblox ID of the responsible moderator for this ban.
     * @param reason               The reason for this ban, or null if none is specified.
     * @param starts               The unix millisecond timestamp for when this ban became active.
     * @param ends                 If not null, the unix millisecond timestamp for when this ban will end.
     * @param linkedTicketId       If not null, the ID of the ticket tied to this ban.
     * @param isLegacy             Is this ban an old legacy ban from the previous system?
     */
    public AWBan(long userId, Long responsibleModerator, String reason, long starts, Long ends, Long linkedTicketId, boolean isLegacy) {
        this.userId = userId;
        this.responsibleModerator = (responsibleModerator != null && responsibleModerator == 0) ? null : responsibleModerator;
        this.reason = reason;
        this.starts = starts;
        this.ends = (ends != null && ends == 0) ? null : ends;
        this.linkedTicketId = (linkedTicketId != null && linkedTicketId == 0) ? null : linkedTicketId;
        this.isLegacy = isLegacy;
    }

    /**
     * Creates an {@link AWBan} object from the provided JSON data and user ID.
     *
     * @param userId The ID of the user associated with the ban.
     * @param json   A {@link JsonObject} containing the ban data. The JSON may include
     *               keys such as "responsible_user" (optional long), "reason" (optional string),
     *               "starts" (long), and "ends" (optional long). Missing optional keys will result in null or default values.
     * @return An {@link AWBan} object populated with data extracted from the provided JSON.
     */
    public static AWBan fromFulfillmentJSON(long userId, JsonObject json) {
        String reason = json.has("reason") ? json.get("reason").getAsString() : null;
        long starts = json.has("started") ?
                json.get("started").getAsLong() :
                json.has("starts") ?
                        json.get("starts").getAsLong() : -1L;

        if (starts == -1L)
            throw new RuntimeException("Missing field \"started\" or \"starts\" in ban fulfillment JSON. Full object: " + json);

        Long responsibleModerator = (json.has("responsible_user") && !json.get("responsible_user").isJsonNull()) ? json.get("responsible_user").getAsLong() : null;
        Long ends = (json.has("ends") && !json.get("ends").isJsonNull()) ? json.get("ends").getAsLong() : null;
        boolean isLegacy = json.has("legacy") && !json.get("legacy").isJsonNull() && json.get("legacy").getAsBoolean();

        return new AWBan(userId, responsibleModerator, reason, starts, ends, null, isLegacy);
    }


    /**
     * Calculates and returns a string representation of the duration of a ban.
     * If the ban has no defined end time, it is represented as "forever".
     * Otherwise, it calculates the duration in days based on the difference
     * between the start and end timestamps.
     *
     * @return A string indicating the duration of the ban in days,
     * or "forever" if the ban has no defined end time.
     */
    public String durationString() {
        if (this.ends == null) {
            return "forever";
        } else {
            long daysRemaining = Math.ceilDiv((this.ends - this.starts), (1000 * 60 * 60 * 24));
            return "for " + daysRemaining + " days";
        }
    }

    public long userId() {
        return this.userId;
    }

    public Long responsibleModerator() {
        return this.responsibleModerator;
    }

    public String reason() {
        return this.reason;
    }

    public long starts() {
        return this.starts;
    }

    public Long ends() {
        return this.ends;
    }

    public boolean isLegacy() {
        return this.isLegacy;
    }

    /**
     * Calculates and returns a string representing the time remaining until the ban ends.
     * If the ban has no defined end time, it returns "An eternity remaining".
     * Otherwise, it computes and returns the remaining time in days.
     *
     * @return A string indicating the remaining time until the ban ends in days,
     * or "An eternity remaining" if there is no defined end time.
     */
    public String remainingString() {
        if (this.ends == null) {
            return "An eternity remaining";
        } else {
            long daysRemaining = Math.ceilDiv((this.ends - System.currentTimeMillis()), (1000 * 60 * 60 * 24));
            return daysRemaining + " days remaining";
        }
    }

    public String displayReasonOrDefault() {
        return (this.reason != null && !this.reason.isBlank()) ? this.reason : "No reason specified";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AWBan) obj;
        return this.userId == that.userId &&
                Objects.equals(this.responsibleModerator, that.responsibleModerator) &&
                Objects.equals(this.reason, that.reason) &&
                this.starts == that.starts &&
                Objects.equals(this.ends, that.ends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId, this.responsibleModerator, this.reason, this.starts, this.ends, this.linkedTicketId);
    }

    @Override
    public String toString() {
        return "AWBan{" +
                "userId=" + this.userId +
                ", responsibleModerator=" + (this.responsibleModerator != null ? this.responsibleModerator : "Unknown") +
                ", reason='" + (this.reason != null ? this.reason : "No reason specified") + '\'' +
                ", starts=" + this.starts +
                ", ends=" + (this.ends != null ? this.ends : "Indefinite") +
                ", linkedTicketId=" + (this.linkedTicketId != null ? this.linkedTicketId : "None") +
                '}';
    }
}
