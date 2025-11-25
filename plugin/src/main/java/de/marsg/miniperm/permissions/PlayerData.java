package de.marsg.miniperm.permissions;

import java.time.Instant;

import org.bukkit.permissions.PermissionAttachment;

public class PlayerData {

    private PermissionGroup group;
    private Instant expiresAt;
    private String language;
    private PermissionAttachment attatchment;

    public PlayerData(PermissionGroup group, PermissionAttachment attatchment, Instant expiresAt) {
        this.group = group;
        this.attatchment = attatchment;
        this.expiresAt = expiresAt;
        this.language = "en";
    }

    public PlayerData(PermissionGroup group, PermissionAttachment attatchment, Instant expiresAt, String language) {
        this.group = group;
        this.attatchment = attatchment;
        this.expiresAt = expiresAt;
        this.language = language;
    }

    public PermissionGroup getGroup() {
        return group;
    }

    public PermissionAttachment getAttachment() {
        return attatchment;
    }

    public Instant getExpirationTimestamp() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public String getLanguage() {
        return language;
    }

    public void updateLanguage(String language) {
        this.language = language;
    }

}
