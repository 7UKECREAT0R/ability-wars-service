package org.lukecreator.aw.data;

/**
 * Represents a change in a player's punches performed manually by a moderator.
 *
 * @param userId               The user affected by this punch update.
 * @param responsibleModerator The Roblox ID of the moderator responsible, if any.
 * @param date                 The timestamp of this update.
 * @param oldPunches           The old value of punches.
 * @param newPunches           The new value of punches.
 */
public record AWPunchUpdate(long userId, Long responsibleModerator, long date, long oldPunches, long newPunches) {
    public AWPunchUpdate(long userId, Long responsibleModerator, long date, long oldPunches, long newPunches) {
        this.userId = userId;
        this.responsibleModerator = (responsibleModerator != null && responsibleModerator == 0) ? null : responsibleModerator;
        this.date = date;
        this.oldPunches = oldPunches;
        this.newPunches = newPunches;
    }
}
