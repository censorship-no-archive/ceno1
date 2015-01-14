package plugins.CeNo.BridgeInterface;

public class Bundle {
	private String content;
	
	public Bundle(String URI) {
		content = "Bundle for URI " + URI;
	}

	public String getContent() {
		return content;
	}
	
	
}
