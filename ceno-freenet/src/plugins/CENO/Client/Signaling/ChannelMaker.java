package plugins.CENO.Client.Signaling;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Client.CENOClient;
import plugins.CENO.Common.Crypto;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.client.InsertException.InsertExceptionMode;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.node.FSParseException;
import freenet.node.RequestClient;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

public class ChannelMaker implements Runnable {
    private static final int MAX_KSK_POLLS = 20;
    
    private FreenetURI signalSSK;
    private FreenetURI signalSSKpub;
    private boolean channelEstablished = false;
    private ChannelStatus channelStatus = ChannelStatus.starting;
    private long lastSynced = 0L;

    public ChannelMaker() {
        this(null, 0L);
    }

    public ChannelMaker(String signalSSKString, long lastSynced) {
        this.lastSynced = lastSynced;
        try {
            if (signalSSKString != null) {
                this.signalSSK = new FreenetURI(signalSSKString);
                this.signalSSKpub = this.signalSSK.deriveRequestURIFromInsertURI();
                channelStatus = ChannelStatus.publishedKSK;
                return;
            }
        } catch (MalformedURLException e) {
            Logger.error(this, "signalSSK read from configuration is not a valid Freenet key");
        } finally {
            FreenetURI newKeyPair[] = CENOClient.nodeInterface.generateKeyPair();
            this.signalSSK = newKeyPair[0];
            this.signalSSKpub = newKeyPair[1];
            this.channelStatus = ChannelStatus.starting;
        }
    }

    @Override
    public void run() {
        if (!checkChannelEstablished()) {
            if(ChannelStatus.isFatalStatus(channelStatus)) {
                return;
            }
            establishChannel();
        } else if (ChannelStatus.sentPrivUSK(channelStatus)) {
            waitForSyn();
        }
    }

    public String getSignalSSK() {
        return signalSSK.toString();
    }

    public long getLastSynced() {
        return lastSynced;
    }

    public boolean isFatal() {
        return ChannelStatus.isFatalStatus(channelStatus);
    }

    public boolean canSend() {
        return ChannelStatus.canSend(channelStatus);
    }

    private boolean checkChannelEstablished() {
        if (System.currentTimeMillis() - lastSynced  > TimeUnit.DAYS.toMillis(30)) {
            return false;
        }

        if(ChannelStatus.sentPrivUSK(channelStatus)) {
            return true;
        }

        return channelEstablished;
    }
    
    private boolean waitForSyn() {
        FreenetURI synURI = new FreenetURI("USK", "syn", signalSSKpub.getRoutingKey(), signalSSKpub.getCryptoKey(), signalSSKpub.getExtra());
        FetchResult fetchResult = null;
        while(!channelEstablished) {
            try {
                fetchResult = CENOClient.nodeInterface.fetchURI(synURI);
            } catch (FetchException e) {
                if(e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
                    synURI = e.newURI;
                } else if(e.isDNF() || e.isFatal()) {
                    try {
                        Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                    } catch (InterruptedException e1) {}
                    continue;
                }
            }
            if(fetchResult != null) {
                try {
                    Long synDate = Long.parseLong(new String(fetchResult.asByteArray()));
                    if (System.currentTimeMillis() - synDate > TimeUnit.DAYS.toMillis(25)) {
                        establishChannel();
                        break;
                    }
                } catch (IOException e) {
                    channelStatus = ChannelStatus.failedToParseSyn;
                    break;
                }
                Logger.normal(this, "Received syn from the bridge - signaling channel established successfully");
                channelStatus = ChannelStatus.syn;
                lastSynced = System.currentTimeMillis();
                channelEstablished = true;
            }
        }
        return channelEstablished;
    }

    private void establishChannel() {
        FreenetURI bridgeKey;
        try {
            bridgeKey = new FreenetURI(CENOClient.getBridgeKey());
        } catch (MalformedURLException e1) {
            Logger.error(this, "Could not calculate the bridge key from the configured SSK: " + e1.getMessage());
            channelStatus = ChannelStatus.fatal;
            return;
        }
        FreenetURI bridgeSignalerURI = new FreenetURI("USK", "CENO-signaler", bridgeKey.getRoutingKey(), bridgeKey.getCryptoKey(), bridgeKey.getExtra());
        FetchResult bridgeSignalFreesite = null;
        while(bridgeSignalFreesite == null) {
            try {
                bridgeSignalFreesite = CENOClient.nodeInterface.fetchURI(bridgeSignalerURI);
            } catch (FetchException e) {
                if (e.mode == FetchException.FetchExceptionMode.PERMANENT_REDIRECT) {
                    bridgeSignalerURI = e.newURI;
                    continue;
                }
                if (e.isFatal()) {
                    channelStatus = ChannelStatus.failedToGetSignalSSK;
                    return;
                }
                Logger.warning(this, "Exception while retrieving the bridge's signal page: " + e.getMessage());
            }
        }
        channelStatus = ChannelStatus.gotSignalSSK;
        SimpleFieldSet sfs = null;
        String question = null;
        String pubKey = null;
        try {
            sfs = new SimpleFieldSet(new String(bridgeSignalFreesite.asByteArray()), false, true, true);
            question = sfs.getString("question");
            pubKey = sfs.getString("pubkey");
        } catch (IOException e) {
            Logger.error(this, "IOException while reading the CENO-signaler page");
        } catch (FSParseException e) {
            Logger.error(this, "Exception while parsing the SFS of the CENO-signaler");
        } finally {
            if (question == null || pubKey == null) {
                // CENO Client won't be able to signal the bridge
                //TODO Terminate plugin
                channelStatus = ChannelStatus.failedToParseSignalSFS;
                return;
            }
        }
        channelStatus = ChannelStatus.puzzleSolved;
        byte[] encReply = null;
        try {
            encReply = Crypto.encrypt(signalSSK.toString().getBytes("UTF-8"), pubKey);
        } catch (GeneralSecurityException e1) {
            Logger.error(this, "General security exception while encrypting response to quiz: " + e1.getMessage());
        } catch (IllegalBase64Exception e1) {
            Logger.error(this, "Could not base64 decrypt bridge's public key : " + e1.getMessage());
        } catch (UnsupportedEncodingException e) {
            Logger.error(this, "UTF-8 Encoding not supported");
        } finally {
            if (encReply == null) {
                channelStatus = ChannelStatus.failedToEncrypt;
                return;
            }
        }
        
        insertSubKSK(question, encReply,  new int[0]);
        waitForSyn();
    }
    
    private void insertSubKSK(String question, byte[] encReply, int[] prevSubKSK) {
        FreenetURI insertedKSK = null;
        int randSubKSK = (int) (Math.random() * MAX_KSK_POLLS);
        try {
            insertedKSK = CENOClient.nodeInterface.insertSingleChunk(new FreenetURI("KSK@" + question + "-" + randSubKSK), encReply, new KSKSolutionPutCallback(question, encReply, new int[]{randSubKSK}));
        } catch (PersistenceDisabledException e) {
            Logger.error(this, "Tried to insert KSK reply with persistence, but persistence is disabled: " + e.getMessage());
            channelStatus = ChannelStatus.failedToPublishKSK;
        } catch (MalformedURLException e) {
            Logger.error(this, "Reply KSK is malformed: " + e.getMessage());
            channelStatus = ChannelStatus.failedToPublishKSK;
        } catch (InsertException e) {
            Logger.error(this, "Error while initializing insertion for KSK reply: " + e.getMessage());
            channelStatus = ChannelStatus.failedToPublishKSK;
        } catch (UnsupportedEncodingException e) {
            Logger.error(this, "UTF-8 Encoding not supported");
            channelStatus = ChannelStatus.failedToPublishKSK;
        } finally {
            if(insertedKSK == null) {
                return;
            }
        }
        channelStatus = ChannelStatus.publishedKSK;
        Logger.normal(this, "Started publishing to KSK solution to the bridge slot " + randSubKSK);
        return;
    }
    
    private class KSKSolutionPutCallback implements ClientPutCallback {
        int[] prevSubKSK;
        byte[] encReply;
        String question;
        
        public KSKSolutionPutCallback(String question, byte[] encReply, int[] prevSubKSK) {
            this.question = question;
            this.encReply = encReply;
            this.prevSubKSK = prevSubKSK;
        }

        @Override
        public void onResume(ClientContext context) throws ResumeFailedException {}

        @Override
        public RequestClient getRequestClient() {
            return CENOClient.nodeInterface.getRequestClient();
        }

        @Override
        public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {}

        @Override
        public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {}

        @Override
        public void onFetchable(BaseClientPutter state) {}

        @Override
        public void onSuccess(BaseClientPutter state) {
            Logger.normal(this,"Inserted private SSK key in the KSK@solution to the puzzle published by the bridge");
        }

        @Override
        public void onFailure(InsertException e, BaseClientPutter state) {
            if (e.getMode() == InsertExceptionMode.COLLISION) {
                boolean subKSKUsed;
                int randSubKSK;
                do {
                    subKSKUsed = false;
                    randSubKSK = (int) (Math.random() * MAX_KSK_POLLS);
                    for (int i = 0; i < prevSubKSK.length ; i++) {
                        if (prevSubKSK[i] == randSubKSK) {
                            subKSKUsed = true;
                            break;
                        }
                    }
                } while (subKSKUsed);
                int[] subKSKUsedExt = new int[prevSubKSK.length + 1];
                System.arraycopy(prevSubKSK,0, subKSKUsedExt, 0, prevSubKSK.length);
                subKSKUsedExt[prevSubKSK.length] = randSubKSK;
                insertSubKSK(question, encReply, subKSKUsedExt);
            } else {
                Logger.error(this, "Failed to publish KSK@solution: " + e.getMessage());
            }
        }
        
    }

}
