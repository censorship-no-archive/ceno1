package plugins.CENO;

import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.l10n.PluginL10n;
import freenet.pluginmanager.FredPluginBaseL10n;
import freenet.support.Logger;

/**
 * Provides the methods for localizing Strings, given that a
 * file for a language exists. It is used internally for
 * translating CENO Error messages and other elements that
 * are expected to be presented to users.
 * 
 * Localization files, in the format of properties files, are
 * stored in {@value #L10nFilesBasePath}.
 */
public class CENOL10n implements FredPluginBaseL10n {

    private static PluginL10n l10n;
    private static final CENOL10n INSTANCE = new CENOL10n();

    public static final String L10nFilesBasePath = "/plugins/CENO/resources/l10n/";

    public static CENOL10n getInstance() {
        return INSTANCE;
    }

    private CENOL10n() {
        l10n = new PluginL10n(this); 
    }

    /**
     * Gets the localized String that corresponds to the given
     * key, for the set language.
     * 
     * @param key the key in the localization file
     * @return the corresponding localized String
     */
    public String getString(String key) {
        return l10n.getBase().getString(key);
    }

    /**
     * Sets the language to be used for localizing Strings
     * by reading an environment variable.
     * **Important:** this function will use the value of
     * the environment variable in the shell that was used
     * for starting the Freenet node.
     * 
     * @param envVar the environment variable to read for
     * setting the language
     */
    public void setLanguageFromEnvVar(String envVar) {
        LANGUAGE lang = getLanguageFromEnvVar(envVar);
        if (lang != l10n.getBase().getSelectedLanguage() && lang != LANGUAGE.UNLISTED) {
            setLanguage(lang);
        }
    }

    /**
     * Sets the language used for localization.
     * 
     * @param newLanguage the new language to use
     * for localizing Strings
     */
    public void setLanguage(LANGUAGE newLanguage) {
        try {
            l10n = new PluginL10n(this, newLanguage);
        } catch (Exception e) {
            Logger.warning(this, "Could not set language to " + newLanguage);
        }
    }

    /**
     * Reads an environment variable and parses it to a Freenet-supported
     * language name. If the environment variable is not set, or cannot
     * be correctly parsed to a language name, ENGLISH is returned by default.
     * 
     * @param envVar the environment variable to read
     * @return the Language name of the parsed environment variable, or
     * ENGLISH if there was an error.
     */
    public LANGUAGE getLanguageFromEnvVar(String envVar) {
        String envVal = System.getenv(envVar);
        if (envVal != null && !envVal.isEmpty()) {
            envVal = envVal.split("-")[0];
        } else {
            envVal = "en";
        }
        LANGUAGE lang = LANGUAGE.mapToLanguage(envVal);
        if (lang == null) {
            lang = LANGUAGE.ENGLISH;
        }
        return lang;
    }

    public String getL10nFilesBasePath() {
        return L10nFilesBasePath;
    }

    public String getL10nFilesMask() {
        return "${lang}.properties";
    }

    public String getL10nOverrideFilesMask() {
        return "${lang}.override.properties";
    }

    public ClassLoader getPluginClassLoader() {
        return CENOL10n.class.getClassLoader();
    }

}
