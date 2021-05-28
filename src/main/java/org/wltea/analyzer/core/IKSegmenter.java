/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.core;

import org.wltea.analyzer.cfg.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * IK分词器主类
 *
 */
public final class IKSegmenter {
	
	//字符窜reader
	private Reader input;
	//分词器上下文
	private AnalyzeContext context;
	//分词处理器列表
	private List<ISegmenter> segmenters;
	//分词歧义裁决器
	private IKArbitrator arbitrator;
    private  Configuration configuration;
	

	/**
	 * IK分词器构造函数
	 * @param input
     */
	public IKSegmenter(Reader input ,Configuration configuration){
		this.input = input;
        this.configuration = configuration;
        this.init();
	}

	
	/**
	 * 初始化
	 */
	private void init(){
		//初始化分词上下文
		this.context = new AnalyzeContext(configuration);
		//加载子分词器
		this.segmenters = this.loadSegmenters();
		//加载歧义裁决器
		this.arbitrator = new IKArbitrator();
	}
	
	/**
	 * 初始化词典，加载子分词器实现
	 * @return List<ISegmenter>
	 */
	private List<ISegmenter> loadSegmenters(){
		List<ISegmenter> segmenters = new ArrayList<ISegmenter>(4);
		//处理字母的子分词器
		segmenters.add(new LetterSegmenter()); 
		//处理中文数量词的子分词器
		segmenters.add(new CN_QuantifierSegmenter());
		//处理中文词的子分词器
		segmenters.add(new CJKSegmenter(configuration.isIncludeSingleChar()));
		return segmenters;
	}
	
	/**
	 * 分词，获取下一个词元
	 * @return Lexeme 词元对象
	 * @throws java.io.IOException
	 */
	public synchronized Lexeme next()throws IOException{
		Lexeme l = null;
		while((l = context.getNextLexeme()) == null ){
			/*
			 * 从reader中读取数据，填充buffer
			 * 如果reader是分次读入buffer的，那么buffer要  进行移位处理
			 * 移位处理上次读入的但未处理的数据
			 */
			int available = context.fillBuffer(this.input);
			if(available <= 0){
				//reader已经读完
				context.reset();
				return null;
				
			}else{
				//初始化指针
				context.initCursor();
				do{
        			//遍历子分词器
        			for(ISegmenter segmenter : segmenters){
        				segmenter.analyze(context);
        			}
        			//字符缓冲区接近读完，需要读入新的字符
        			if(context.needRefillBuffer()){
        				break;
        			}
   				//向前移动指针
				}while(context.moveCursor());
				//重置子分词器，为下轮循环进行初始化
				for(ISegmenter segmenter : segmenters){
					segmenter.reset();
				}
			}
			//对分词进行歧义处理
			this.arbitrator.process(context, configuration.isUseSmart());
			//将分词结果输出到结果集，并处理未切分的单个CJK字符
			context.outputToResult();
			//记录本次分词的缓冲区位移
			context.markBufferOffset();			
		}
		return l;
	}

	/**
     * 重置分词器到初始状态
     * @param input
     */
	public synchronized void reset(Reader input) {
		this.input = input;
		context.reset();
		for(ISegmenter segmenter : segmenters){
			segmenter.reset();
		}
	}
}
