package org.elasticsearch.plugin.analysis.ik;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.IkAnalyzerProvider;
import org.elasticsearch.index.analysis.IkTokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;


public class AnalysisIkPlugin extends Plugin implements AnalysisPlugin {

	public static String PLUGIN_NAME = "analysis-ik";

	@Override
	public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
		Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();

		extra.put("ik_smart", IkTokenizerFactory::getIkSmartTokenizerFactory);
		extra.put("ik_max_word", IkTokenizerFactory::getIkTokenizerFactory);

		return extra;
	}

	@Override
	public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
		Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> extra = new HashMap<>();

		extra.put("ik_smart", IkAnalyzerProvider::getIkSmartAnalyzerProvider);
		extra.put("ik_max_word", IkAnalyzerProvider::getIkAnalyzerProvider);

		return extra;
	}

}
