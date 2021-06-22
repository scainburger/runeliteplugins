package net.runelite.client.plugins.autofillpassword;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autofillPassword")
public interface AutofillPasswordConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "stringUsername",
            name = "Username",
            secret = true,
            description = "Enter your username here"
    )
    default String stringUsername() { return ""; }

    @ConfigItem(
            position = 2,
            keyName = "stringPassword",
            name = "Password",
            secret = true,
            description = "Enter your password here"
    )
    default String stringPassword() { return ""; }
}