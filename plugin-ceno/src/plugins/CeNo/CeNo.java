package pulgins.CeNo

    public class CeNo FredPlugin {

    private final static Logger Logger LOGGER = Logger.getLogger(CeNo.class.getName());
    private PulginRespirator pluginRespirator;

    public static final String pluginUri = "/plugins/plugins.CeNo.CeNo";
	public static final String pluginName = "CeNo";

    /**
       setup the web interface in FProxy
       but we don't really need a web inteface as we don't use FProxy 
       per sa
     */
    private void setupWebInterface()
    {
		webInterface = new WebInterface(this, pluginRespirator.getHLSimpleClient(), pluginRespirator.getToadletContainer());
		webInterface.load();

        /*PluginContex pluginContext = new PluginContext(pluginRespirator);
        this.webInterface = new WebInterface(plginContext);

        pluginRespirator.getPageMaker().addNavigationCategory(basePath + "/", "WebOfTrust.menunName.name", "WebOfTrust.menuName.tooltip", this);
        ToadletContainer tc = pluginRespirator.getToadletContainer();

        ///pages
        Overview oc = new Overview(this, pluginRespirator.getHLSimpleClient(), basePath, db);*/
                                 
        
    }

    public void runPlugin(PluginRespirator pr)
    {
        pluginRespirator = pr;
        setupWebInterface();
           
    }

    public void terminate()
    {
        Logger.normal(this, pluginName + " terminated.");
    }

