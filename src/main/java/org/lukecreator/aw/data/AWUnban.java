package org.lukecreator.aw.data;

public record AWUnban(long userId, Long responsibleModerator, long date) {
    public AWUnban(long userId, Long responsibleModerator, long date) {
        this.userId = userId;
        this.responsibleModerator = (responsibleModerator != null && responsibleModerator == 0) ? null : responsibleModerator;
        this.date = date;
    }
}
