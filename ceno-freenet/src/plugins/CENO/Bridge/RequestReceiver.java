package plugins.CENO.Bridge;

public class RequestReceiver {
	
	private static volatile RequestReceiver instance = new RequestReceiver();

	private RequestReceiver() {
	}

	public static RequestReceiver getInstance() {
		return instance;
	}

}
