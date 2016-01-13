package plugins.CENO.Client.Signaling;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import plugins.CENO.Client.CENOClient;

public class Channel {

    public static boolean insertBatch(String string) {
        if (!CENOClient.channelMaker.canSend()) {
            return false;
        }

        try {
            FreenetURI signalSSK = new FreenetURI(CENOClient.channelMaker.getSignalSSK());
            FreenetURI reqURI = new FreenetURI("USK", "req",signalSSK.getRoutingKey(), signalSSK.getCryptoKey(), signalSSK.getExtra());
            CENOClient.nodeInterface.insertSingleChunk(reqURI, string,
                    CENOClient.nodeInterface.getVoidPutCallback("Inserted request batch in the channel", "Failed to insert request batch"));
        } catch (UnsupportedEncodingException e) {
            return false;
        } catch (MalformedURLException e) {
            return false;
        } catch (InsertException e) {
            return false;
        } catch (PersistenceDisabledException e) {
            return false;
        }
        return true;
    }

}
