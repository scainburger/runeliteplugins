package net.runelite.client.plugins.partyreadycheck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("partyreadycheck")
public interface PartyReadyCheckConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "alternateSounds",
            name = "Use alternate sounds",
            description = "Whether to use the alternate sounds over the default Runescape-sourced sounds"
    )
    default boolean alternateSounds() { return false; }
}