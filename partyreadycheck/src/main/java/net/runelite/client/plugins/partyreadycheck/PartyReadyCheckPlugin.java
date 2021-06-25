package net.runelite.client.plugins.partyreadycheck;

import com.google.inject.Provides;
import com.openosrs.client.game.SoundManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ws.PartyService;
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

    private static final int PARTY_LIST_ID_TOB_HEADER = 1835019;
    private static final int PARTY_LIST_ID_TOB = 1835020;

    private Timer timer;
    private int rcTicksRemaining = -1;

    private Clip readyCheckStartSound = null;

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (rcTicksRemaining == 0) {
            rcTicksRemaining = -1;
            playSound("fail.wav", SoundEffectID.PRAYER_DEACTIVE_VWOOP);
            sendChatMessage("The ready check timed out.");
            resetFrame();
        }
        if (rcTicksRemaining > 0 ) {
            rcTicksRemaining--;
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

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
    }

    private AudioFormat getOutFormat(AudioFormat inFormat)
    {
        int ch = inFormat.getChannels();
        float rate = inFormat.getSampleRate();
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
    }

    public void playSound(String customSound, int fallbackSound)
    {

        if (config.alternateSounds()) {
            try {
                Clip clip = AudioSystem.getClip();
                InputStream fileStream = new BufferedInputStream(
                        PartyReadyCheckPlugin.class.getResourceAsStream(customSound)
                );
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(fileStream);
                AudioFormat outFormat = PartyReadyCheckPlugin.this.getOutFormat(clip.getFormat());
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
                if (clip != null) {
                    clip.open(inputStream);
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                        muteControl.setValue(false);
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        int soundVol = (int) Math.round( client.getPreferences().getSoundEffectVolume() / 1.27);
                        float newVol = (float) (Math.log((double) soundVol/100) / Math.log(10.0) * 20.0);
                        gainControl.setValue(newVol);
                    }
                }
                clip.start();
                return;
            } catch (Exception e) {
                log.warn("Could not play custom sound file: " + e.getMessage());
            }
        }
        client.playSoundEffect(fallbackSound);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if   ( client.getWidget(PARTY_LIST_ID_TOB_HEADER) == null
            || client.getWidget(PARTY_LIST_ID_TOB_HEADER).getText().equals("No party")
            || client.getWidget(PARTY_LIST_ID_TOB_HEADER).isHidden()) {
            return;
        }
        String msg = chatMessage.getMessage().toUpperCase(Locale.ROOT).trim();
        if (msg.equals("R") || msg.equals("UN R"))
        {
            raidingPartyWidget = client.getWidget(PARTY_LIST_ID_TOB);// Player list sub-widget on TOB party frame
            if (raidingPartyWidget == null || raidingPartyWidget.isHidden())
                return;

            String[] playerNames = raidingPartyWidget.getText().split("<br>");
            String outputText = "";

            String senderName = chatMessage.getName().replace("\u00A0", " ");

            for (int i = 0; i < playerNames.length; i++)
            {
                String name = playerNames[i];
                if (name.equals(senderName) && msg.equals("R"))
                { // Un-ready player is now ready
                    outputText = outputText + name + " (R)";

                    if (rcTicksRemaining == -1) {
                        rcTicksRemaining = 17;
                        sendChatMessage(name + " has started a ready check.");
                        playSound("start.wav", SoundEffectID.GE_ADD_OFFER_DINGALING);
                    }

                }
                else if (name.equals(senderName + " (R)") && msg.equals("UN R"))
                { // Ready player is now un-ready
                    outputText = outputText + senderName; // restore to original
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
                    // If anyone not ready, stop
                    return;
                }
            }

            // If we get here all players are ready
            sendChatMessage("All party members are ready!");
            playSound("success.wav", SoundEffectID.GE_COIN_TINKLE);
            resetFrame();
            rcTicksRemaining = -1;

        }
    }

    private void resetFrame() {
        String[] playerNames = raidingPartyWidget.getText().split("<br>");
        String outputText = "";

        for (int i = 0; i < playerNames.length; i++) {
            String name = playerNames[i];
            if (name.endsWith(" (R)")) {
                name = name.substring(0, name.length() - 4);
            }
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