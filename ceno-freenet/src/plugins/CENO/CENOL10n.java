package plugins.CENO;

import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.l10n.PluginL10n;
import freenet.pluginmanager.FredPluginBaseL10n;
import freenet.support.Logger;

public class CENOL10n implements FredPluginBaseL10n {

	private static PluginL10n l10n;

	public CENOL10n(String envVar) {
		CENOL10n.l10n = new PluginL10n(this);
		setLanguageFromEnvVar(envVar);
	}

	public String getString(String key) {
		return CENOL10n.l10n.getBase().getString(key);
	}

	public void setLanguage(LANGUAGE newLanguage) {
		try {
			CENOL10n.l10n = new PluginL10n(this, newLanguage);
		} catch (Exception e) {
			Logger.warning(this, "Could not set language to " + newLanguage);
		}
	}

	public void setLanguageFromEnvVar(String envVar) {
		String lang = System.getenv(envVar);
		if (lang == null || lang.isEmpty()) {
			lang = "en";
		} else {
			lang = lang.split("-")[0];
		}
		setLanguage(LANGUAGE.mapToLanguage(lang));
	}

	public String getL10nFilesBasePath() {
		return "/plugins/CENO/resources/l10n/";
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

	public static String get(String key) {
		if (CENOL10n.l10n == null) {
			return "Localization base not initialized";
		}
		return CENOL10n.l10n.getBase().getString(key);
	}

}
