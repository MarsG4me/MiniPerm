import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.GameMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;
import de.marsg.miniperm.permissions.PermissionGroup;

public class TestWithMockBukkit {

    private ServerMock server;
    private MiniPerm plugin;

    private MockedStatic<DBMgr> dbMock;

    private MockedStatic<CompletableFuture> cfMock;

    public PlayerMock getCustomPlayer(String name) {
        PlayerMock playerMock = new PlayerMock(server, name, UUID.randomUUID());

        playerMock.setGameMode(GameMode.CREATIVE);

        return playerMock;
    }

    @BeforeEach
    void setUp() {
        // mock the completableFutures
        cfMock = mockStatic(CompletableFuture.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getMethod().getName().equals("runAsync") && invocation.getArguments().length == 1) {
                    // Execute the Runnable passed to runAsync immediately
                    ((Runnable) invocation.getArgument(0)).run();
                    return CompletableFuture.completedFuture(null);
                }
                // Execute all other CompletableFuture methods normally
                return invocation.callRealMethod();
            }
        });
        // Mock DBMgr methods
        dbMock = mockStatic(DBMgr.class);
        dbMock.when(() -> DBMgr.setup(any(MiniPerm.class))).thenReturn(true);
        dbMock.when(() -> DBMgr.addGroup(anyString(), anyString(), anyInt(), anyBoolean())).thenReturn(1);
        dbMock.when(() -> DBMgr.deleteGroup(anyString())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.addPermission(anyInt(), anyString())).thenReturn(true);
        dbMock.when(() -> DBMgr.removePermission(anyInt(), anyString())).thenReturn(true);
        dbMock.when(() -> DBMgr.getAllGroups()).thenReturn(Map.of("default", new Object[] { 1, "[Default]", 0, true }));
        dbMock.when(() -> DBMgr.getGroupsPermission(anyInt())).thenReturn(Set.of());
        dbMock.when(() -> DBMgr.setUsersGroup(any(), anyInt(), any())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.updateUsersLanguage(any(), anyString())).thenAnswer(i -> null);
        dbMock.when(() -> DBMgr.getUsersGroup(any())).thenReturn(new Object[] { "default", null, "en" });

        // Start the mock server
        server = MockBukkit.mock();

        // Mock MiniPerm Plugin
        plugin = MockBukkit.load(MiniPerm.class);

        // Skip some time to load all classes
        server.getScheduler().performTicks(5);

        // --- Add default group (DBMgr.addGroup mocked already) ---
        PermissionGroup defaultGroup = new PermissionGroup(1, "default", "[Default]", 0, true, HashSet.newHashSet(2));
        plugin.getPermissionsMgr().addGroup(defaultGroup);
        plugin.getPermissionsMgr().addPermission("default", "test.default");

        PermissionGroup adminGroup = new PermissionGroup(1, "admin", "[Admin]", 100, false, HashSet.newHashSet(2));
        plugin.getPermissionsMgr().addGroup(adminGroup);
        plugin.getPermissionsMgr().addPermission("admin", "test.admin");
        plugin.getPermissionsMgr().addPermission("admin", "miniperm.admin");

        // Skip some time to load all classes
        server.getScheduler().performTicks(5);
    }

    @AfterEach
    void tearDown() {
        // Stop the mock server
        MockBukkit.unmock();
        dbMock.close();
        cfMock.close();
    }

    @Test
    void testLeaveMessage() {
        // A test for the join message was planed but always returned null for
        // player.nextMessage()

        PlayerMock player = getCustomPlayer("Steve");

        server.addPlayer(player);
        plugin.getPermissionsMgr().setPlayersGroup(player, "default");

        player.disconnect();
        assertEquals("[Default] §cSteve left the game.", player.nextMessage());

    }

    @Test
    void testNoPermissionSetPlayersGroup() {

        PlayerMock player = getCustomPlayer("Steve");

        server.addPlayer(player);
        plugin.getPermissionsMgr().setPlayersGroup(player, "default");

        player.performCommand("miniperm user set_group Steve admin");

        assertEquals("§4You don't have permission to use this command!", player.nextMessage());

    }

    @Test
    void testHasPermissionCmd() {

        PlayerMock steve = getCustomPlayer("Steve");
        PlayerMock alex = getCustomPlayer("Alex");

        server.addPlayer(steve);
        plugin.getPermissionsMgr().setPlayersGroup(steve, "default");

        server.addPlayer(alex);
        plugin.getPermissionsMgr().setPlayersGroup(alex, "admin");

        steve.performCommand("miniperm test miniperm.admin");

        assertEquals("§cYou don't have the permission: miniperm.admin", steve.nextMessage());

        alex.performCommand("miniperm test miniperm.admin");

        assertEquals("§aYou have the permission: miniperm.admin", alex.nextMessage());
    }

    @Test
    void testUserInfoCmd() {

        PlayerMock steve = getCustomPlayer("Steve");
        PlayerMock alex = getCustomPlayer("Alex");

        server.addPlayer(steve);
        plugin.getPermissionsMgr().setPlayersGroup(steve, "default");

        server.addPlayer(alex);
        plugin.getPermissionsMgr().setPlayersGroup(alex, "admin");

        // Check Steve's info
        alex.performCommand("miniperm user info Steve");

        assertEquals("§6Player 'Steve' is part in the 'default' group.", alex.nextMessage());
    }

    @Test
    void testCheckUserAddsPermissionCmd() {

        PlayerMock steve = getCustomPlayer("Steve");
        PlayerMock alex = getCustomPlayer("Alex");

        server.addPlayer(steve);
        plugin.getPermissionsMgr().setPlayersGroup(steve, "default");

        server.addPlayer(alex);
        plugin.getPermissionsMgr().setPlayersGroup(alex, "admin");

        assertFalse(steve.hasPermission("test.123"));

        steve.performCommand("miniperm test test.123");
        assertEquals("§cYou don't have the permission: test.123", steve.nextMessage());

        // give Steve the permission
        alex.performCommand("miniperm permissions default add test.123");
        assertEquals("§aYou added the permission test.123", alex.nextMessage());

        // Skip some time to let async part fix the permission
        server.getScheduler().performTicks(5);

        assertTrue(steve.hasPermission("test.123"));

        steve.performCommand("miniperm test test.123");
        assertEquals("§aYou have the permission: test.123", steve.nextMessage());
    }

    @Test
    void testCheckSetGroupCmd() {

        PlayerMock steve = getCustomPlayer("Steve");
        PlayerMock alex = getCustomPlayer("Alex");

        server.addPlayer(steve);
        plugin.getPermissionsMgr().setPlayersGroup(steve, "default");

        server.addPlayer(alex);
        plugin.getPermissionsMgr().setPlayersGroup(alex, "admin");

        // Checks before new group assignment
        steve.performCommand("whoami");
        assertEquals("§6Your group is 'default'.", steve.nextMessage());

        // give Steve the admin rank
        alex.performCommand("miniperm user set_group Steve admin 10s");

        // Skip some time to let async part do stuffs
        server.getScheduler().performTicks(5);
        // dates change but message structure not
        assertTrue(alex.nextMessage().startsWith("$aYou set Steve group to admin until 2"));

        // Check if he received the rank
        steve.performCommand("whoami");
        // dates change but message structure not
        assertTrue(steve.nextMessage().startsWith("§6Your group is 'admin'. And you are part of it until 2"));

        // Skip some time to let async part do stuffs (and run out of time)
        server.getScheduler().performTicks(15 * 20);
        // Info message of player being removed from group admin
        assertEquals("§5The time is up, you are now no longer in the group 'admin'.", steve.nextMessage());

        // Check if rank was removed again
        steve.performCommand("whoami");
        assertEquals("§6Your group is 'default'.", steve.nextMessage());
    }
}
