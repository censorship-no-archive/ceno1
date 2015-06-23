package plugins.CENO.Client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.db4o.ObjectContainer;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;

public class ClientGetSyncCallback implements ClientGetCallback {
	private final CountDownLatch fetchLatch = new CountDownLatch(1);
	private String fetchResult;
	private FetchException fe;

	public void onMajorProgress(ObjectContainer container) {
	}

	public void onSuccess(FetchResult result, ClientGetter state, ObjectContainer container) {
		try {
			fetchResult = new String(result.asByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		fetchLatch.countDown();
	}

	public void onFailure(FetchException e, ClientGetter state,
			ObjectContainer container) {
		//TODO Handle local cache lookup exceptions
		fe = e;
		fetchLatch.countDown();
	}

	public String getResult(long timeout, TimeUnit unit) throws FetchException {
		try {
			fetchLatch.await(timeout, unit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		if (fe != null && !fe.isDNF()) {
			throw fe;
		}
		return fetchResult;
	}

}
