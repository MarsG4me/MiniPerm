# MiniPerm
Coding challenge for application as a dev for Playlegend.net


## IMPORTANT
This is the first push, not all requirements are implemented yet **AND** the code needs some cleanup!!


-----------------------------------

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
>miniperm user info \<player_name> <br>
>miniperm user set_group \<player_name> \<group_name> [time] <br>
>miniperm user remove_group \<player_name>  <br>

* Create rank signs <br>
>miniperm create_sign 

* Test if user has permission using `#hasPermission` <br>
>miniperm test \<permission> 

* Language Management <br>
>miniperm language \<language> 

* Self group check <br>
>whoami 

## Notes and stuff
* Only UUID is important for the permissions; Switching from/to an offline server (For testing!) will give a player a different UUID
* all ingame messages are configurable (console is fixed and in english!)
* pom was AI generated; Especially the inclusion of the DB Drivers created a lot of friction
* AI was also used for a README spell check as well as the spellcheck of the configurable output texts
* Rank signs only update when the player is online (The rank will be blank after a server reboot until the player joins)
* Code coverage can be seen after running `mvn test` in `plugin>target>site>jacoco>index.html`