package plugins.CENO.Bridge;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;

import plugins.CENO.CENOJettyHandler;
import plugins.CENO.Bridge.BundleInserter.InsertCallback;
import plugins.CENO.Bridge.BundlerInterface.Bundle;

import com.db4o.ObjectContainer;

import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;

/* ------------------------------------------------------------ */
/** CeNo Plugin handler for requests to cache bundles
 * 	
 * CacheInsertHandler listens to {@link CENOBridge#bundleInserterPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class BundleInserterHandler extends CENOJettyHandler {

	// Insertion requests time out after 5 mins
	static final long insertionRequestTimeout = 5 * 60 * 1000;

	public class HandlerInsertCallback extends InsertCallback {
		private Request baseRequest;
		private Continuation continuation;
		private HttpServletResponse response;

		public HandlerInsertCallback(Request request, Continuation continuation, HttpServletResponse response) {
			super();
			this.baseRequest = request;
			this.continuation = continuation;
			this.response = response;
		}

		@Override
		public void onSuccess(BaseClientPutter state, ObjectContainer container) {
			super.onSuccess(state, container);
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			try {
				response.getWriter().println("stored");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			baseRequest.setHandled(true);
			continuation.complete();
		}

		@Override
		public void onFailure(InsertException e, BaseClientPutter state, ObjectContainer container) {
			super.onFailure(e, state, container);
			try {
				writeError(baseRequest, response, "not stored");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			continuation.complete();
		}

	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException {
		// Only POST requests at /store route will be handled
		if (!request.getMethod().equals("POST")) {
			writeError(baseRequest, response, "Not a POST request at /store route");
			return;
		}

		// Get the data from the POST request
		//TODO Add support for multi-part POST requests
		JSONObject requestJSON;
		try {
			requestJSON = readJSONbody(request.getReader());
		} catch (ParseException e) {
			writeError(baseRequest, response, "Could not parse JSON");
			return;
		}
		if(!requestJSON.containsKey("url")) {
			writeError(baseRequest, response, "No url attribute in request body");
			return;
		}
		if(!requestJSON.containsKey("bundle")) {
			writeError(baseRequest, response, "No bundle attribute in request body");
			return;
		}

		String urlParam = requestJSON.get("url").toString();
		if((urlParam == null) || urlParam.isEmpty()) {
			writeError(baseRequest, response, "Invalid url attribute");
			return;
		}

		Bundle bundle = new Bundle(urlParam);
		if ((requestJSON.get("bundle") != null)) {
			bundle.setContent(requestJSON.get("bundle").toString());
		} else {
			writeError(baseRequest, response, "Bundle attribute was null");
			return;
		}

		if (!bundle.getContent().isEmpty()) {
			Continuation continuation = ContinuationSupport.getContinuation(baseRequest);
			continuation.setTimeout(insertionRequestTimeout);
			if (continuation.isExpired())
			{
				writeError(baseRequest, response, "Request timed out");
				return;
			}
			try {
				BundleInserter.insertBundle(urlParam, bundle, new HandlerInsertCallback(baseRequest, continuation, response));
			} catch (InsertException e) {
				writeError(baseRequest, response, "Error during insertion");
				return;
			} catch (Exception e1) {
				writeError(baseRequest, response, "Exception during insertion: " + e1.getMessage());
			}
			continuation.suspend();
		} else {
			writeError(baseRequest, response, urlParam);
		}
	}
}
