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
 * 
 */
package org.wltea.analyzer.core;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

/**
 * 
 * 中文数量词子分词器
 */
class CN_QuantifierSegmenter implements ISegmenter{
	
	//子分词器标签
	static final String SEGMENTER_NAME = "QUAN_SEGMENTER";
	
	//中文数词
	private static String Chn_Num = "一二两三四五六七八九十零壹贰叁肆伍陆柒捌玖拾百千万亿拾佰仟萬億兆卅廿";//Cnum
	private static Set<Character> ChnNumberChars = new HashSet<Character>();
	static{
		char[] ca = Chn_Num.toCharArray();
		for(char nChar : ca){
			ChnNumberChars.add(nChar);
		}
	}

	private final Configuration cfg;

	/*
	 * 词元的开始位置，
	 * 同时作为子分词器状态标识
	 * 当start > -1 时，标识当前的分词器正在处理字符
	 */
	private int nStart;
	/*
	 * 记录词元结束位置
	 * end记录的是在词元中最后一个出现的合理的数词结束
	 */
	private int nEnd;

	//待处理的量词hit队列
	private List<Hit> countHits;
	
	
	CN_QuantifierSegmenter(Configuration cfg){
		nStart = -1;
		nEnd = -1;
		this.countHits  = new LinkedList<Hit>();
		this.cfg = cfg;
	}
	
	/**
	 * 分词
	 */
	public void analyze(AnalyzeContext context) {
		//处理中文数词
		this.processCNumber(context);
		//处理中文量词
		this.processCount(context);
		
		//判断是否锁定缓冲区
		if(this.nStart == -1 && this.nEnd == -1	&& countHits.isEmpty()){
			//对缓冲区解锁
			context.unlockBuffer(SEGMENTER_NAME);
		}else{
			context.lockBuffer(SEGMENTER_NAME);
		}
	}
	

	/**
	 * 重置子分词器状态
	 */
	public void reset() {
		nStart = -1;
		nEnd = -1;
		countHits.clear();
	}
	
	/**
	 * 处理数词
	 */
	private void processCNumber(AnalyzeContext context){
		if(nStart == -1 && nEnd == -1){//初始状态
			if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType() 
					&& ChnNumberChars.contains(context.getCurrentChar())){
				//记录数词的起始、结束位置
				nStart = context.getCursor();
				nEnd = context.getCursor();
			}
		}else{//正在处理状态
			if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType() 
					&& ChnNumberChars.contains(context.getCurrentChar())){
				//记录数词的结束位置
				nEnd = context.getCursor();
			}else{
				//输出数词
				this.outputNumLexeme(context);
				//重置头尾指针
				nStart = -1;
				nEnd = -1;
			}
		}
		
		//缓冲区已经用完，还有尚未输出的数词
		if(context.isBufferConsumed() && (nStart != -1 && nEnd != -1)){
			//输出数词
			outputNumLexeme(context);
			//重置头尾指针
			nStart = -1;
			nEnd = -1;
		}	
	}
	
	/**
	 * 处理中文量词
	 * @param context
	 */
	private void processCount(AnalyzeContext context){
		// 判断是否需要启动量词扫描
		if(!this.needCountScan(context)){
			return;
		}
		
		if(CharacterUtil.CHAR_CHINESE == context.getCurrentCharType()){
			
			//优先处理countHits中的hit
			if(!this.countHits.isEmpty()){
				//处理词段队列
				Hit[] tmpArray = this.countHits.toArray(new Hit[this.countHits.size()]);
				for(Hit hit : tmpArray){
					hit = Dictionary.getDictionary(cfg.getRemoteDictName()).matchWithHit(context.getSegmentBuff(), context.getCursor() , hit);
					if(hit.isMatch()){
						//输出当前的词
						Lexeme newLexeme = new Lexeme(context.getBufferOffset() , hit.getBegin() , context.getCursor() - hit.getBegin() + 1 , Lexeme.TYPE_COUNT);
						context.addLexeme(newLexeme);
						
						if(!hit.isPrefix()){//不是词前缀，hit不需要继续匹配，移除
							this.countHits.remove(hit);
						}
						
					}else if(hit.isUnmatch()){
						//hit不是词，移除
						this.countHits.remove(hit);
					}					
				}
			}				

			//*********************************
			//对当前指针位置的字符进行单字匹配
			Hit singleCharHit = Dictionary.getDictionary(cfg.getRemoteDictName()).matchInQuantifierDict(context.getSegmentBuff(), context.getCursor(), 1);
			if(singleCharHit.isMatch()){//首字成量词词
				//输出当前的词
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , context.getCursor() , 1 , Lexeme.TYPE_COUNT);
				context.addLexeme(newLexeme);

				//同时也是词前缀
				if(singleCharHit.isPrefix()){
					//前缀匹配则放入hit列表
					this.countHits.add(singleCharHit);
				}
			}else if(singleCharHit.isPrefix()){//首字为量词前缀
				//前缀匹配则放入hit列表
				this.countHits.add(singleCharHit);
			}
			
			
		}else{
			//输入的不是中文字符
			//清空未成形的量词
			this.countHits.clear();
		}
		
		//缓冲区数据已经读完，还有尚未输出的量词
		if(context.isBufferConsumed()){
			//清空未成形的量词
			this.countHits.clear();
		}
	}
	
	/**
	 * 判断是否需要扫描量词
	 * @return
	 */
	private boolean needCountScan(AnalyzeContext context){
		if((nStart != -1 && nEnd != -1 ) || !countHits.isEmpty()){
			//正在处理中文数词,或者正在处理量词
			return true;
		}else{
			//找到一个相邻的数词
			if(!context.getOrgLexemes().isEmpty()){
				Lexeme l = context.getOrgLexemes().peekLast();
				if((Lexeme.TYPE_CNUM == l.getLexemeType() ||  Lexeme.TYPE_ARABIC == l.getLexemeType())
					&& (l.getBegin() + l.getLength() == context.getCursor())){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 添加数词词元到结果集
	 * @param context
	 */
	private void outputNumLexeme(AnalyzeContext context){
		if(nStart > -1 && nEnd > -1){
			//输出数词
			Lexeme newLexeme = new Lexeme(context.getBufferOffset() , nStart , nEnd - nStart + 1 , Lexeme.TYPE_CNUM);
			context.addLexeme(newLexeme);
			
		}
	}

}
