package plugins.CENO.Bridge.BundlerInterface;

public class BundleRequest {

    public static Bundle requestURI(String URI) {
        Bundle bundle = new Bundle(URI);
        bundle.requestFromBundlerSafe();
        return bundle;
    }
}
