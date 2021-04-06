# TeamPlugin
* `[]` = optional
* `<>` = obligatory

### All players Commands
* `/ut` Tells you the name of the unit you are controlling.
* `/vnw` (VoteNewWave) Vote for Sending a new Wave.
* `/maps` List all maps on server.


### Admin Only commands
* `/team [teamname]` This command lets admins change teams.
* `/spectate` Enter/leave spectate mode.
* `/ac <message...>` Send a message in the admin chat.
* `/kick <username...>` Kick a person by name.
* `/pardon <ID>` Pardon a votekicked player by ID and allow them to join again.
* `/ban <type-id|name|ip> <username|IP|ID...>`  Ban a person.
* `/unban <ip|ID>` Unban a person.
- [ ]  `/players [all|connect|ban]` Gives the list of players according to the type of filter given.
- [ ]  `/info-all <username|ID>` List all information related to the given player.

### Feedback
Open an issue if you have a suggestion.

### Releases
Prebuild relases can be found [here](https://github.com/Susideur/moreCommands/releases)

### Building a Jar 
You have just run `build.bat` and the plugin will compile automatically.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `plugins` command.
