package net.runelite.client.plugins.partyreadycheck;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.devtools.WidgetField;
import net.runelite.client.ws.PartyMember;
import net.runelite.client.ws.PartyService;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeoutException;

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

    Widget raidingPartyWidget;

    @Inject
    private Client client;

    @Inject
    private PartyService partyService;

    @Inject
    private PartyReadyCheckConfig config;

    private boolean timerRunning = false;

    private static final int PARTY_LIST_ID_TOB = 1835020;

    private Timer timer;

    private int soundToPlay = 0;

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (soundToPlay > 0) {
            client.playSoundEffect(soundToPlay);
            soundToPlay = 0;
        }
    }

    //
    // Below taken from DailyTasksPlugin
    @Inject
    private ChatMessageManager chatMessageManager;

    private void sendChatMessage(String chatMessage)
    {
        final String message = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(chatMessage)
                .build();

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(message)
                        .build());
    }
    // End stolen code :)
    //

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        //log.info("\"" + chatMessage.getMessage() + "\"");
        String msg = chatMessage.getMessage().toUpperCase(Locale.ROOT);
        if (msg.equals("R") || msg.equals("UN R"))
        {
            log.info("Timer is running? " + timerRunning);

            raidingPartyWidget = client.getWidget(PARTY_LIST_ID_TOB);// Player list sub-widget on TOB party frame
            if (raidingPartyWidget == null || raidingPartyWidget.isHidden())
            {
                return;
            }

            String[] playerNames = raidingPartyWidget.getText().split("<br>");
            String outputText = "";

            for (int i = 0; i < playerNames.length; i++)
            {
                String name = playerNames[i];
                //log.info("Checking if \"" + chatMessage.getName() + "\" equals \"" + name + "\"");
                if (name.equals(chatMessage.getName()) && msg.equals("R"))
                { // Un-ready player is now ready
                    outputText = outputText + name + " (R)";

                    if (timerRunning) {
                        timer.cancel();
                        timer.purge();
                        log.info("Stopping the timer...");
                        timerRunning = false;
                    }
                    else {
                        // Starting ready check...
                        soundToPlay = SoundEffectID.GE_ADD_OFFER_DINGALING;
                    }

                    timer = new Timer("ReadyCheckTimer");
                    timer.schedule(new TimerTask() {
                                public void run() {
                                    timerRunning = false;
                                    soundToPlay = SoundEffectID.PRAYER_DEACTIVE_VWOOP;
                                    log.info("The ready check timed out...");
                                    sendChatMessage("The ready check timed out...");
                                    resetFrame();
                                }
                            }, 60000L);
                    timerRunning = true;
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

            //
            // Now check if all players are ready
            //

            playerNames = raidingPartyWidget.getText().split("<br>");
            for (int i = 0; i < playerNames.length; i++)
            {
                String name = playerNames[i];
                if (!name.equals("-") && !name.endsWith(" (R)")) {
                    //log.info("Someone is not ready - \"" + name + "\"");
                    return;
                }
            }

            // We are ready
            //log.info("All party members are ready!");
            sendChatMessage("All party members are ready!");
            soundToPlay = SoundEffectID.GE_COIN_TINKLE;
            resetFrame();
            if (timerRunning) {
                timer.cancel();
                timer.purge();
                //log.info("Stopping the timer...");
            }

        }
    }

    private void resetFrame() {
        String[] playerNames = raidingPartyWidget.getText().split("<br>");
        String outputText = "";

        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name.endsWith(" (R)")) {
                log.info(name + " is ready so we are removing the (R)");
                name = name.substring(0, name.length() - 4);
            }
            log.info("The man to add is \"" + name + "\"");
            outputText = outputText + name;
            if (i < 4) outputText += "<br>";

            raidingPartyWidget.setText(outputText);
        }
    }

    @Provides
    PartyReadyCheckConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PartyReadyCheckConfig.class);
    }
}