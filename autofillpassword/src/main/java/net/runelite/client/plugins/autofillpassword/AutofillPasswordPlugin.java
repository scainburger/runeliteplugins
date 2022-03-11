package net.runelite.client.plugins.autofillpassword;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.input.KeyListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.util.OSType;
import org.pf4j.Extension;
import net.runelite.client.input.KeyManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

@Extension
@PluginDescriptor(
        name = "Autofill Password",
        description = "Plugin to automatically fill password and login",
        tags = {"menu"},
        loadWhenOutdated = true,
        enabledByDefault = true
)

@Slf4j
public class AutofillPasswordPlugin extends Plugin implements KeyListener {

    @Inject
    private Client client;

    @Inject
    private AutofillPasswordConfig config;

    @Inject
    private KeyManager keyManager;

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(this);
        client.getCanvas().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                fillPassword();
            }
        });
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("Key Pressed!");
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
            fillPassword();
    }

    public void fillPassword() {
        if (client.getGameState() != GameState.LOGIN_SCREEN)
            return;

        System.out.println("Doing it");
        if (config.stringUsername() != "")
            client.setUsername(config.stringUsername());

        if (config.stringPassword() != "")
            client.setPassword(config.stringPassword());
    }

    @Override
    public boolean isEnabledOnLoginScreen()
    {
        return true;
    }

    @Provides
    AutofillPasswordConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutofillPasswordConfig.class);
    }

    // Unused overrides
    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
}