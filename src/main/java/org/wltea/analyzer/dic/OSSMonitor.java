package org.wltea.analyzer.dic;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;


/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/11/8
 */

public class OSSMonitor implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(OSSMonitor.class.getName());

	/**
	 * 资源属性
	 */
	private String eTags;

	/**
	 *
	 */
	private String endpoint;



	public OSSMonitor(String endpoint) {
		this.endpoint = endpoint;
		this.eTags = null;
	}
	/**
	 * 监控流程：
	 *  ①从响应中获取Last-Modify、ETags字段值，判断是否变化
	 *  ②如果未变化，休眠1min，返回第①步
	 *  ③如果有变化，重新加载词典
	 * 	④休眠1min，返回第①步
	 */

	@Override
	public void run() {
		OssDictClient ossDictClient = OssDictClient.getInstance();
		try {
			ObjectMetadata objectMetadata = ossDictClient.getObjectMetaData(this.endpoint);
			logger.info(String.format("the endpoint is %s", this.endpoint));
			if (objectMetadata != null && !objectMetadata.getETag().equalsIgnoreCase(eTags)) {
				//reload dict
				// 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
				Dictionary.getSingleton().reLoadMainDict();
				eTags = objectMetadata.getETag();
			}
			if (objectMetadata != null && Strings.isNotBlank(eTags) && AnalysisIkPlugin.clusterService.state().nodes().getLocalNode() != null) {
                String localNodeName = AnalysisIkPlugin.clusterService.localNode().getName();
			    if (objectMetadata.getUserMetadata() == null || objectMetadata.getUserMetadata().get(localNodeName.toLowerCase()) == null
                        || !objectMetadata.getUserMetadata().get(localNodeName.toLowerCase()).equals(eTags)) {
                        logger.info(String.format("node name is %s and will upload etags to oss file! The eTags is %s", localNodeName, eTags));
                        List<String> otherNodeNameList = new ArrayList<>();
                        for (DiscoveryNode node : AnalysisIkPlugin.clusterService.state().nodes()) {
							if (!node.getName().equals(localNodeName)) {
								otherNodeNameList.add(node.getName().toLowerCase());
							}
						}
						AnalysisIkPlugin.clusterService.state().getNodes();
						ossDictClient.updateObjectUserMetaInfo(this.endpoint, otherNodeNameList, localNodeName.toLowerCase(), eTags);
			    }
            }
		} catch (OSSException e) {
			if (!e.getErrorCode().equals("404")) {
				logger.error("get dict from oss failed or update file meta data failed!", e);
			}
		} catch (ClientException | IOException e) {
			logger.error("oss client exception !", e);
		}
	}

}
