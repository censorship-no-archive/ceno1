package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Common.Crypto;
import plugins.CENO.Common.GetSyncCallback;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.keys.FreenetURI;
import freenet.support.Logger;


class ChannelMakerListener implements Runnable {    
    static final long KSK_POLLING_PAUSE = TimeUnit.MINUTES.toMillis(5);

    private String puzzleAnswer;
    private FreenetURI channelMakingKSK;
    private PrivateKey asymPrivKey;

    private volatile boolean continueLoop;

    public ChannelMakerListener(String puzzleAnswer, int subKsk, PrivateKey asymPrivKey) throws MalformedURLException {
        this.puzzleAnswer = puzzleAnswer;
        this.asymPrivKey = asymPrivKey;
        channelMakingKSK = new FreenetURI("KSK@" + this.puzzleAnswer + "-" + subKsk);
        continueLoop = true;
    }

    @Override
    public void run() {
        try {
            int window = 0;
            while(continueLoop) {
                byte[] kskContent = null;
                GetSyncCallback getSyncCallback = new GetSyncCallback(CENOBridge.nodeInterface.getRequestClient());
                try {
                    CENOBridge.nodeInterface.distFetchURI(channelMakingKSK, getSyncCallback);
                    kskContent = getSyncCallback.getResult(10 * KSK_POLLING_PAUSE, TimeUnit.MILLISECONDS);
                } catch (FetchException e) {
                    // TODO Fine-grain log messages according to FetchException codes
                    if(e.mode == FetchExceptionMode.RECENTLY_FAILED) {
                        window++;
                    }
                    Logger.normal(ChannelMakerListener.class, "Exception while fetching KSK clients use for making channels: " + e.getShortMessage());
                }

                if (kskContent != null) {
                    if (window > 0) {
                        window--;
                    }
                    Logger.minor(this, "Congestion window for fetching KSKs without getting Recently Failed exceptions set to: " + window + " minutes");

                    String decKskContent = null;
                    try {
                        decKskContent = new String(Crypto.decrypt(kskContent, asymPrivKey), "UTF-8");
                    } catch (GeneralSecurityException e) {
                        Logger.error(this, "General Security Exception while decrypting KSK reply from client");
                        continue;
                    }

                    Logger.normal(ChannelMakerListener.class, "A client has posted information for establishing a signaling channel");
                    ChannelManager.getInstance().addChannel(decKskContent);
                    //TODO Consume slot
                    continueLoop = false;
                }

                // Pause the looping thread
                Thread.sleep(KSK_POLLING_PAUSE + TimeUnit.MINUTES.toMillis(window));
            }
        } catch (InterruptedException e) {
            continueLoop = false;
        } catch (IOException e) {
            Logger.warning(this, "IOException while trying to create channel from KSK response");
            continueLoop = false;
        }
    }

    public void stopListener() {
        continueLoop = false;
    }

}
