package org.elasticsearch.index.analysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
	private final IKAnalyzer analyzer;

	public IkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings, boolean useSmart) {
		super(indexSettings, name, settings);

		Configuration configuration = new Configuration(env, settings).setUseSmart(useSmart);

		analyzer = new IKAnalyzer(configuration);
	}

	public static IkAnalyzerProvider getIkSmartAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
		return new IkAnalyzerProvider(indexSettings, env, name, settings, true);
	}

	public static IkAnalyzerProvider getIkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
		return new IkAnalyzerProvider(indexSettings, env, name, settings, false);
	}

	@Override
	public IKAnalyzer get() {
		return this.analyzer;
	}
}
