# MiniPerm
Coding challenge for application as a dev for Playlegend.net

## Given Requirements
#### Legend
✅ is done <br>
🕒 started <br>
☑️ not yet done <br>
🟥 will not be done

### Minimum
* ✅ Groups can be created and managed in the game

* The group must have at least the following properties:
    * ✅ Name
    * ✅ Prefix

* Players should be able to be assigned to a group:
    * ✅ Permanent
    * ✅ Temporary - with a time specification. It should be possible to specify 4 days, 7 minutes, and 23 seconds in-game.

* ✅ Prefix of the group should be displayed in the chat and when joining the server.

* ✅ When the player is assigned a new group, this should change **immediately** (player should not be kicked).

* ✅ All messages should be customizable in a configuration file.

* ✅ A command tells the player his current group and, if applicable, how long he still has it.

* ✅ It should be possible to add one or more signs that display an individual player's information such as name & rank.
* ✅ All necessary information is stored in a **relational database** (NOT the configurable texts).

* ✅ All Unit Tests pass:
    * **+15% Code coverage** (must include most important parts).


### Bonus
* ✅ Permissions can be defined for a group and should be assigned to the player accordingly. Query via `#hasPermission` should work.

* 🟥 “*” permission.

* ✅ Support for multiple languages.

* ☑️ Tablist with the respective group sorted.

* ☑️ Scoreboard with the respective group.


-----------------------------------

## Setup
### Database Setup
As you need to set up a JDBC URL, here is an example to use PostgreSQL databases (it is also the only type which has drivers inserted):

> **PostgreSQL:** `jdbc:postgresql://localhost:5432/db_name`

### Permissions

* Give all permissions:
> `miniperm.admin`

* Allow listing/creating/deleting groups:
> `miniperm.groups`

* Allow listing/creating/deleting permissions for groups:
> `miniperm.permissions`

* Allow info of user and set/removing user from groups:
> `miniperm.user`

* Allow creating rank signs
> `miniperm.signs`

### Commands
#### Legend: [optional] -- \<variable>

* Group Management<br>
> miniperm groups list <br>
> miniperm groups create \<group_name> \<weight> \<is_default> \<prefix> <br>
> miniperm groups delete \<group_name><br>

* Permission Management <br>
> miniperm permissions \<group_name> list <br>
> miniperm permissions \<group_name> add \<permission> <br>
> miniperm permissions \<group_name> remove \<permission> <br>

* User Management <br>
> miniperm user info \<player_name> <br>
> miniperm user set_group \<player_name> \<group_name> [time] <br>
> miniperm user remove_group \<player_name> <br>

* Create rank signs <br>
> miniperm create_sign <br>

* Test if user has permission using `#hasPermission` <br>
> miniperm test \<permission> <br>

* Language Management <br>
> miniperm language \<language> <br>

* Self group check <br>
> whoami 

## Notes and stuff
* Only UUID is important for the permissions; switching from/to an offline server (for testing!) will give a player a different UUID.
* All in-game messages are configurable (console is fixed and in English!).
* POM was AI generated; especially the inclusion of the DB drivers created a lot of friction.
* AI was also used for a README spell check as well as the spell check of the configurable output texts.
* Rank signs only update when the player is online (the rank will be blank after a server reboot until the player joins).
* Code coverage can be seen after running `mvn test` in `plugin > target > site > jacoco > index.html`.

## Own Thoughts

**These are the ones I had at the end of the day, I was way too tired to continue... The side scoreboard would have been easily accomplished. The top one probably with another full day.**

* Optimize the rank signs; they are definitely janky but work.
* Scoreboard could have been easily done but there is no time left (would have used Teams scoreboard and display them on the side per player).
* Updates would be triggered like the rank signs.
* Scoreboard in TAB would be approached like this: (teams that have a name of WEIGHT_TEAM and then make them display in the tablist; here it would be one scoreboard team per group instead of player).
* Better tests; using MockBukkit helped a lot getting >15% code coverage as most of the code is for the command logic.
* Better organization of the Commands; maybe even split them further down → one class per subcommand.