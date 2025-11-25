import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;
import de.marsg.miniperm.permissions.PermissionGroup;
import de.marsg.miniperm.permissions.PermissionsMgr;
import de.marsg.miniperm.scheduler.GroupExpirationScheduler;

class TestPermissionssMgr {

    private MiniPerm plugin;
    private Player player;
    private PermissionAttachment attachment;
    private PermissionsMgr perms;
    private MockedStatic<DBMgr> dbMock;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setup() {
        // Mock DBMgr methods
        dbMock = mockStatic(DBMgr.class);
        dbMock.when(() -> DBMgr.addGroup(anyString(), anyString(), anyInt(), anyBoolean())).thenReturn(1);
        dbMock.when(() -> DBMgr.deleteGroup(anyString())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.addPermission(anyInt(), anyString())).thenReturn(true);
        dbMock.when(() -> DBMgr.removePermission(anyInt(), anyString())).thenReturn(true);
        dbMock.when(() -> DBMgr.getAllGroups()).thenReturn(Map.of("default", new Object[] { 1, "[Player]", 0, true }));
        dbMock.when(() -> DBMgr.getGroupsPermission(anyInt())).thenReturn(Set.of());
        dbMock.when(() -> DBMgr.setUsersGroup(any(), anyInt(), any())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.updateUsersLanguage(any(), anyString())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.getUsersGroup(any())).thenReturn(new Object[] { "default", null, "en" });

        // Mock Bukkit Scheduler (to run in sync not async)
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            Runnable run = invocation.getArgument(1);
            run.run(); // run now
            return null;
        }).when(scheduler).runTask(any(), any(Runnable.class));

        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

        // Mock MiniPerm Plugin
        plugin = mock(MiniPerm.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getExpirationScheduler()).thenReturn(mock(GroupExpirationScheduler.class));

        // Mock Player
        player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        attachment = mock(PermissionAttachment.class);
        when(player.addAttachment(any())).thenReturn(attachment);

        // --- Instantiate PermissionsMgr ---
        perms = new PermissionsMgr(plugin);

        // --- Add default group (DBMgr.addGroup mocked already) ---
        PermissionGroup defaultGroup = new PermissionGroup(1, "default", "[Default]", 0, true, Set.of("perm.basic"));
        // This call triggers DBMgr.addGroup but the mock swallows it
        perms.addGroup(defaultGroup);
    }

    @AfterEach
    void cleanup() {
        // Close all static mocks to get a clean start for the next test
        dbMock.close();
        bukkitMock.close();
    }

    /**
     * Tests if the player receives the default group if they have none
     */
    @Test
    void testGivePlayerDefaultGroup() {
        boolean result = perms.setPlayersGroup(player, "default");
        Assertions.assertTrue(result, "setPlayersGroup should never be false!");
    }

    /**
     * Tests the group exists check
     */
    @Test
    void testDoesGroupExist() {
        Assertions.assertTrue(perms.doesGroupExist("default"));
        Assertions.assertFalse(perms.doesGroupExist("other"));
    }

    /**
     * Tests if the language update is pushed to db and saved locally
     */
    @Test
    void testUpdateLanguage() {
        perms.setPlayersGroup(player, "default");

        // Mock CompletableFuture to run NOW like if it is sync and not async
        @SuppressWarnings("rawtypes")
        MockedStatic<CompletableFuture> cfMock = mockStatic(CompletableFuture.class);
        cfMock.when(() -> CompletableFuture.runAsync(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run(); // execute immediately
                    return CompletableFuture.completedFuture(null);
                });

        perms.updateLanguage(player, "de");

        // Verify that the update was "send" to DB
        dbMock.verify(() -> DBMgr.updateUsersLanguage(any(), eq("de")));

        // Close the mock
        cfMock.close();

        // ensure the update worked
        assertEquals("de", perms.getPlayersData(player).getLanguage());
    }

}