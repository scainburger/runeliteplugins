package net.runelite.client.plugins.partyreadycheck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("partyreadycheck")
public interface PartyReadyCheckConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "useAlternateSounds",
            name = "Use alternate sounds",
            description = "Place custom sounds in your \".runelite/partyreadycheck\" folder to use them.<br><br>" +
                    "Valid sound files:<br>" +
                    "\"start.wav\" - Played when ready check begins<br>" +
                    "\"success.wav\" - Played when all players are ready<br>" +
                    "\"fail.wav\" - Played when ready check times ot <b> "
    )
    default boolean useAlternateSounds() { return false; }
}