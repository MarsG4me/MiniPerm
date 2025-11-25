package de.marsg.miniperm.permissions;

import java.util.HashSet;
import java.util.Set;

public class PermissionGroup {

    private int id;
    private String name;
    private String prefix;
    private int weight;
    private boolean defaultGroup;
    private Set<String> permissions;

    /**
     * This is the creator to use when generating a new group
     * 
     * @param id
     * @param name
     * @param prefix
     * @param weight
     * @param isDefaultGroup
     */
    public PermissionGroup(int id, String name, String prefix, int weight, boolean isDefaultGroup) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.weight = weight;
        this.defaultGroup = isDefaultGroup;
        this.permissions = HashSet.newHashSet(4);
    }

    /**
     * This is the creator to use when loading the groups from
     * DB
     * 
     * @param id
     * @param name
     * @param prefix
     * @param weight
     * @param isDefaultGroup
     * @param permissions
     */
    public PermissionGroup(int id, String name, String prefix, int weight, boolean isDefaultGroup,
            Set<String> permissions) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.weight = weight;
        this.defaultGroup = isDefaultGroup;
        this.permissions = permissions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (this.id == -1) {
            this.id = id;
        }
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void removeDefaultFlag() {
        defaultGroup = false;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * @param permission
     * @return TRUE if the permission was removed; FALSE if it never existed
     */
    public boolean removePermission(String permission) {
        return permissions.remove(permission);
    }

    /**
     * @param permission
     * @return TRUE if the permission was added; FALSE if it was present before
     */
    public boolean addPermission(String permission) {
        return permissions.add(permission);
    }

}
