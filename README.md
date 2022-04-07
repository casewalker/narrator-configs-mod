# Narrator Configs Mod

Minecraft comes with a builtin narrator. For this narrator, there are three narration options available:

 * Narrates All
 * Narrates Chat
 * Narrates System
 
There are Minecraft Community Feedback posts requesting for the game to expand on what narrations already exist within
Minecraft (see [here](https://feedback.minecraft.net/hc/en-us/community/posts/360050276431) and
[here](https://feedback.minecraft.net/hc/en-us/community/posts/360061320391)). As well, there is an *Accessibility Plus*
mod which aims to tackle many of the issues pointed out in those feedback posts (you can find it
[here](https://github.com/LuisSanchez-Dev/AccessibilityPlus)).

However, new narrations aside, some users find the existing narration options to be too limited. For example, some users
may feel that "_Narrates All_" contains too much narration for them, but that "_Narrates Chat_" also misses important
system messages that come through the chat display. I have created a simple mod for this exact issue (see
[here](https://github.com/casewalker/narrate-chat-mod)). But some users may still want to fully customize the narrator
in ways that are not covered by the above options.

This mod attempts to give fine-grained control to Minecraft players to define exactly which narrations they want to hear
and which they do not. Note that this mod is built on top of [Fabric](https://fabricmc.net/).

## Configuration

The mod expects to find a configuration file in the `config` directory (either `narratorconfigsmod.yml` or
`narratorconfigsmod.json`). The mod has five configuration properties:

* `modEnabled`: Whether the mod is enabled (if the mod is disabled, it will do nothing)
* `chatEnabled`: Whether standard user chat messages are enabled
* `enabledPrefixes`: Using the language translation files, enable specific "key prefixes" to allow matching messages to
be narrated
* `disabledPrefixes`: If a key-prefix is useful but too generic, this property can disable other/more specific
key-prefixes
* `enabledRegularExpressions`: If messages are not covered by keys in the language files, enable specific messages by
providing the full regular expressions to match against

An example `YAML` configuration file:

```yaml
# Enable the mod so that mod-effects occur
modEnabled: yes
# Enable chat so that chat messages are narrated
chatEnabled: yes
# Enable specific system messages based on matching their keys' prefixes in the Minecraft language files:
enabledPrefixes:
  # match the "joined", "joined (renamed)", and "left" multiplayer messages
  - multiplayer.player.
  # match incoming and outgoing whispers
  - commands.message.display.
  # match when a player completes advancements, challenges, and goals
  - chat.type.advancement.
  # match all player death messages
  - death.
  # match messages related to player sleep
  - sleep.
```

## License

Licensed under the MIT License (MIT). Copyright © 2021 Case Walker.
