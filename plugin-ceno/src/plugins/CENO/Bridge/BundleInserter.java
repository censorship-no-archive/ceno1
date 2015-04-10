package plugins.CENO.Bridge;

import com.db4o.ObjectContainer;

import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;

public class BundleInserter {
	
	public class InsertCallback implements ClientPutCallback {

		public void onMajorProgress(ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state,
				ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onGeneratedMetadata(Bucket metadata,
				BaseClientPutter state, ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onFetchable(BaseClientPutter state,
				ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onSuccess(BaseClientPutter state, ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onFailure(InsertException e, BaseClientPutter state,
				ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

	}

	public static void insert(String urlRequested) {
		// TODO Auto-generated method stub
		
	}
	
	

}
