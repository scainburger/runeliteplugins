package net.runelite.client.plugins.autofillpassword;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;

import javax.inject.Inject;

@Extension
@PluginDescriptor(
        name = "Autofill Password",
        description = "Plugin to automatically fill password and login",
        tags = {"menu"},
        loadWhenOutdated = true,
        enabledByDefault = true
)

@Slf4j
public class AutofillPasswordPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private AutofillPasswordConfig config;

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
        {
            if (config.stringUsername() != "")
                client.setUsername(config.stringUsername());

            if (config.stringPassword() != "")
                client.setPassword(config.stringPassword());
        }
    }

    @Provides
    AutofillPasswordConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutofillPasswordConfig.class);
    }
}