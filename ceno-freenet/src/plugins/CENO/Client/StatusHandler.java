package plugins.CENO.Client;

import java.util.concurrent.TimeUnit;

import net.minidev.json.JSONObject;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.FreenetInterface.ConnectionOverview.NodeConnections;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

/**
 * Handler for requests to the /status path. Responsible for returning
 * the Freenet agents' status according to the documentation JSON schema.
 */
public class StatusHandler extends AbstractCENOClientHandler {

	private static Long noPeersTimer = null;

	@Override
	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		NodeConnections nodeConnections = CENOClient.nodeInterface.getConnections();
		if (nodeConnections.getCurrent() == 0) {
			StatusHandler.noPeersTimer = (StatusHandler.noPeersTimer == null) ? System.currentTimeMillis() : StatusHandler.noPeersTimer;
			if (System.currentTimeMillis() - StatusHandler.noPeersTimer > TimeUnit.MINUTES.toMillis(5)) {
				// The node is not connected to any peers for longer than 5 mins.
				// Could it be a firewall/connectivity issue?
				return returnStatus("error", new CENOException(CENOErrCode.LCS_NODE_NOT_ENOUGH_PEERS).getMessage());
			}
		}

		// If the Freenet node is connected to less than 3 peers, the process will be slow
		// and we inform the users appropriately
		if (nodeConnections.getCurrent() < 3) {
			return returnStatus("warning", new CENOException(CENOErrCode.LCS_NODE_INITIALIZING).getMessage());
		}
		
		StatusHandler.noPeersTimer = null;
		return returnStatus("okay", "Freenet node connected to " + nodeConnections.getCurrent() + " peers");
	}

	@Override
	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		// Won't handle POST requests
		return "StatusHandler: POST request received on /status path";
	}

	private String returnStatus(String status, String message) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("message", message);
		return jsonResponse.toJSONString();
	}

}
