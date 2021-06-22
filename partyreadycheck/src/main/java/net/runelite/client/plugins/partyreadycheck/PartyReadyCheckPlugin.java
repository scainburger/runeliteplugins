package net.runelite.client.plugins.partyreadycheck;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.devtools.WidgetField;
import net.runelite.client.ws.PartyMember;
import net.runelite.client.ws.PartyService;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Extension
@PluginDescriptor(
        name = "Party Ready Check",
        description = "Display an alert and a timer to check ready status of your party",
        tags = {"menu"},
        loadWhenOutdated = true,
        enabledByDefault = true
)

@Slf4j
public class PartyReadyCheckPlugin extends Plugin {

    public static int partySize = 0;
    private final ArrayList<String> playerList = new ArrayList<>();

    @Inject
    private Client client;

    @Inject
    private PartyService partyService;

    @Inject
    private PartyReadyCheckConfig config;

    @Subscribe
    public void onGameTick(GameTick event)
    {
        for (int v = 330; v < 335; ++v) {
            String name = Text.standardize(client.getVarcStrValue(v));
            if (!Strings.isNullOrEmpty(name) && !playerList.contains(name)) {
                playerList.add(name);
                partySize = playerList.size();
            }
        }
    }

    @Override
    protected void shutDown()
    {
        partySize = 0;
        playerList.clear();
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        log.info("\"" + chatMessage.getMessage() + "\"");
        String msg = chatMessage.getMessage().toUpperCase(Locale.ROOT);
        if (msg.equals("R") || msg.equals("UN R"))
        {

            Widget raidingPartyWidget = client.getWidget(1835017); // Player list sub-widget on TOB party frame
            if (raidingPartyWidget == null || raidingPartyWidget.isHidden())
            {
                return;
            }

            String[] playerNames = raidingPartyWidget.getText().split("<br>");
            String outputText = "";

            for (int i = 0; i < playerNames.length; i++)
            {
                String name = playerNames[i];
                log.info("Checking if \"" + chatMessage.getName() + "\" equals \"" + name + "\"");
                if (name.equals(chatMessage.getName()) && msg.equals("R"))
                { // Un-ready player is now ready
                    outputText = outputText + name + " (R)";
                }
                else if (name.equals(chatMessage.getName() + " (R)") && msg.equals("UN R"))
                { // Ready player is now un-ready
                    outputText = outputText + chatMessage.getName(); // restore to original
                }
                else
                { // No state change
                    outputText = outputText + name;
                }
                if (i<4) outputText += "<br>";
            }

            raidingPartyWidget.setText(outputText);
        }
    }

    @Provides
    PartyReadyCheckConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PartyReadyCheckConfig.class);
    }
}