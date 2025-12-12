## Features

- Timer that shows on the Actionbar.
- Increasing / Decreasing Mode
- Colors, Animation, Animation Speed is customizable
- Timer Targets, to execute commands when the timer hits a specific time
- Savable Timer
- Maximum Time Display (00:10 / 00:15)
- Perfect for Minigames or Challanges
- Reset Command that can reset (DELETE!!) worlds via command or on boot / shutdown

## How to use

1. Download the jar file and place it in your `plugins/` folder
2. Restart the Server
3. Configure the Timer, either via command or via the config file `plugins/Timer/config.yml`
4. Set the timer mode `/timer mode <up|down>` Default: `up`
5. Change the animation or colors `/timer animation <color1|color2|type|speed|duration>`

- Change the colors `/timer animation <color1|color2> <#FF0000|red>
- Change the animation type `/timer animation type <gradient|wave|pulse|rainbow|still>`
- Change the animation speed `/timer aninmation speed <0.1-10.0>`
- Change the animation duration when setting the time `/timer aninmation duration <1-100>`

6. Start the timer using `/timer start`, you can pause / stop it at any time
7. Configure timer targets

- Add a timer Target `/timer target add <Name/Id> <time|0|10s> <command>`
- Remove a timer Target `/timer target remove <Name/Id>`
- List all Targets `/timer target list`

8. Add a new custom timer with `/timer create <Name> <global|player|team>`

- Manage that timer with `/timer use <Name> <...>`

9. Change config entries for reset command in the `config.yml` file.

- Change `worlds-to-delete` to delete more worlds on reset (Supports **_*_** Wildcard)
- Change `delete-on-boot` or `delete-on-shutdown` to delete worlds on boot or shutdown
