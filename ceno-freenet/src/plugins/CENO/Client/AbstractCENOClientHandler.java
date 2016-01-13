package plugins.CENO.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.minidev.json.JSONObject;
import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import freenet.pluginmanager.FredPluginHTTP;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

/**
 * Abstract class for CENO Client handlers that use fred's servlet
 */
public abstract class AbstractCENOClientHandler implements FredPluginHTTP {

    public abstract String handleHTTPGet(HTTPRequest request) throws PluginHTTPException;
    public abstract String handleHTTPPost(HTTPRequest request) throws PluginHTTPException;

    /**
     * Returns the content of a static resource file
     * 
     * @param filename static file in the resources to serve
     * @return the content of the filename, or throws a CENOException
     * if the file does not exist or could not be read
     */
    protected String printStaticHTML(String filename) {
        InputStream is = AbstractCENOClientHandler.class.getResourceAsStream(filename);
        if (is == null) {
            return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_STATIC_NOT_FOUND));
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = "";
        StringBuilder htmlContent = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                htmlContent.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_STATIC_IO));
        }
        return htmlContent.toString();
    }

    /**
     * Returns the content of a static resource file,
     * replacing all occurrences of a String with another.
     * 
     * @param filename the resource file
     * @param target the String to be replaced
     * @param replacement the String to replace target
     * @return
     */
    protected String printStaticHTMLReplace(String filename, String target, String replacement) {
        return printStaticHTML(filename).replace(target, replacement);
    }

    /**
     * Check whether the "client" parameter is set to "html",
     * so that we respond to requests with an HTML page
     * rather than with a JSON Object.
     * 
     * @param request the request to be checked
     * @return <code>true</code> if the client parameter is set
     * to "html", <code>false</code> otherwise
     */
    protected boolean isClientHtml(HTTPRequest request) {
        String clientType = request.getParam("client");
        if (clientType.compareToIgnoreCase("html") == 0) {
            return true;
        }
        return false;
    }

    /**
     * Compose a String that represents a JSON Object 
     * in the format of exchanging error codes among agents,
     * according to the CENO Protocol specification:
     * 
     * @return
     * {
     *   "errorCode": [corresponding code from the errorConditions doc],
     *   "errMsg": [localized informative error message]
     * }
     */
    private String returnErrorJSON(CENOException cenoEx) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("errCode", cenoEx.getErrCode().getDocCode());
        jsonResponse.put("errMsg", cenoEx.getMessage());
        return jsonResponse.toJSONString();
    }

    /**
     * Compose a String response including a CENOException,
     * to be returned to another CENO agent.
     * 
     * @param cenoEx the CENOException to include in the response
     * @param clientIsHtml whether the client supports HTML only
     * @return
     * If the client is HTML, the response will be an HTML message.
     * Otherwise, if the client supports JSON, the format of the
     * String adheres to the CENO Protocol specification:<br>
     * {
     *   "errorCode": [corresponding code from the errorConditions doc],
     *   "errMsg": [localized informative error message]
     * }
     */
    protected String returnError(CENOException cenoEx, boolean clientIsHtml) {
        if (clientIsHtml) {
            return cenoEx.getMessage();
        } else {
            return returnErrorJSON(cenoEx);
        }
    }

}
