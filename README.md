# moreCommands Plugin

## All players Commands
* `/ut` Tells you the name of the unit you are controlling.
* `/vnw` (VoteNewWave) Vote for Sending a new Wave.
* `/maps` List all maps on server.
* `/info-all [ID|username...]` Get all the player information.
* `/rtv` Rock the vote to change map.
* `/rainbow` Give your username a rainbow animation.
* `/dm <username> <message...>` Send a message to a player. (replace all spaces with '_' in the name) 

### Admin Only Commands
* `/team [teamname]` This command lets admins change teams.
* `/spectate` Enter/leave spectate mode.
* `/am <message...>` Send a message as admin.
* `/kick <ID|username...>` Kick a person by name or ID.
* `/pardon <ID>` Pardon a votekicked player by ID and allow them to join again.
* `/ban <type-id|name|ip> <username|IP|ID...>`  Ban a person.
* `/unban <ip|ID>` Unban a person.
* `/players <all|online|ban>` Gives the list of players according to the type of filter given.
* `/kill [username...]` Kill a player.
* `/tp <name|x,y> [to_name|x,y]` Teleport to a coordinate or a player. (replace all spaces with '_' in the name) 
* `/core <small|medium|big>` Spawn a core to your corrdinate.
* `/tchat <on|off>` Enabled/disabled the tchat.

### Server Commands
* `unban-all [y|n]` Unban all ID and IP.
* `auto-pause <on|off>` Pause the game if no one is connected.
* `tchat <on|off>` Enabled/disabled the tchat.


### Feedback
Open an issue if you have a suggestion.

### Releases
Prebuild relases can be found [here](https://github.com/Susideur/moreCommands/releases)

### Building a Jar 
You have just run `build.bat` and the plugin will compile automatically.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `plugins` command.

### Thanks to...
This plugin is a grouping of other plugins, with modifications or upgrades.

J-VdS - [TeamPlugin](https://github.com/J-VdS/TeamPlugin)<br>
Phinner - [BetterCommands](https://github.com/Phinner/BetterCommands)<br>
L0615T1C5-216AC-9437 - [admin-tools](https://github.com/L0615T1C5-216AC-9437/admin-tools)<br>
Shadow53 - [MindustryAdminPlugin](https://github.com/Shadow53/MindustryAdminPlugin)<br>
Gee-aitcH - [GHMP-VoteNewWave](https://github.com/Gee-aitcH/GHMP-VoteNewWave)<br>
mayli - [RockTheVotePlugin](https://github.com/mayli/RockTheVotePlugin)<br>
nichrosia - [autopause-plugin](https://github.com/nichrosia/autopause-plugin)<br>
OceanPandorum - [PandorumPlugin](https://github.com/OceanPandorum/PandorumPlugin)
