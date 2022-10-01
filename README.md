# Narrator Configs Mod

Minecraft comes with a builtin narrator. For this narrator, there are three narration options available:

 * Narrates All
 * Narrates Chat
 * Narrates System
 
There are Minecraft Community Feedback posts requesting for the game to expand on what narrations already exist within
Minecraft (see [here](https://feedback.minecraft.net/hc/en-us/community/posts/360050276431) and
[here](https://feedback.minecraft.net/hc/en-us/community/posts/360061320391)). As well, there is an *Accessibility Plus*
mod which aims to tackle many of the issues pointed out in those feedback posts (which can be found
[here](https://github.com/LuisSanchez-Dev/AccessibilityPlus)).

However, new narrations aside, some users find the existing narration options to be too limited. For example, some users
may feel that "_Narrates All_" contains too much narration for them, but that "_Narrates Chat_" also misses important
system messages that come through the chat display. A simpler mod was created for this exact issue (see
[here](https://github.com/casewalker/narrate-chat-mod)). But some users may still want to fully customize the narrator
in ways that are not covered by any of the above options.

This mod attempts to give fine-grained control to Minecraft players to define exactly which narrations they want to hear
and which they do not.

Note that this mod is built on top of [Fabric](https://fabricmc.net/).

## Configuration

The mod expects to find a configuration file in the `config` directory (either `narratorconfigsmod.yml` or
`narratorconfigsmod.json`). The mod has four configuration properties:

* `chatEnabled`: Whether standard user chat messages are enabled
* `enabledPrefixes`: Using the language translation files, enable specific "key prefixes" to allow matching messages to
be narrated (_if the text sent to the narrator begins with a translation, it should be narrated_)
* `disabledPrefixes`: If a key-prefix is useful but too generic, this property can disable other/more specific
key-prefixes (it can be used at the same time as `enabledPrefixes` and `enabledRegularExpressions`)
* `enabledRegularExpressions`: If messages are not covered by keys in the language files or a user wants to control
exactly what text-matches should get narrated, enable specific messages by providing the full regular expressions to
match against

> **_NOTE:_** The prefixes described above are found in the language files that come built-in in Minecraft. For
> convenience, an example US-English language file is included in this repository under
> [src/main/resources/language_examples/en_us.json](src/main/resources/language_examples/en_us.json)


An example `YAML` configuration file:

```yaml
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

In order to use the mod, a new narrator option inside the _Options..._ > _Accessibility Settings..._ > _Narrator_ 
settings was added. Click through until the Narrator is set to "_**Custom Configs**_".

## License

Licensed under the MIT License (MIT). Copyright © 2022 Case Walker.
