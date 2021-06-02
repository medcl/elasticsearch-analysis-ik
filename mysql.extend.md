IK Analysis 扩展词新增mysql同步来源
=============================

- 支持启动全量加载扩展词
- 支持热更新扩展词

> mysql 扩展词表结构


```mysql

CREATE TABLE `es_lexicon` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '词库id',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `lexicon_text` varchar(40) NOT NULL COMMENT '词条关键词',
  `lexicon_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0扩展词库 1停用词库',
  `lexicon_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '词条状态 0正常 1暂停使用',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '作废标志 0正常 1作废',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES远程扩展词库表'
```



```IKAnalyzer.cfg.xml```


```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<comment>IK Analyzer 扩展配置</comment>
	<!--用户可以在这里配置自己的扩展字典 -->
	<entry key="ext_dict"></entry>
	 <!--用户可以在这里配置自己的扩展停止词字典-->
	<entry key="ext_stopwords"></entry>
	<!--用户可以在这里配置远程扩展字典 -->
	<!-- <entry key="remote_ext_dict">words_location</entry> -->
	<!--用户可以在这里配置远程扩展停止词字典-->
	<!-- <entry key="remote_ext_stopwords">words_location</entry> -->
	<!-- 连接地址 如果未配置,则不开启数据库同步 -->
	<entry key="db_url"><![CDATA[jdbc:mysql://10.1.11.134:3306/post_bar?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&connectTimeout=60000&socketTimeout=60000&autoReconnect=true&failOverReadOnly=false&useSSL=true&useUnicode=true]]></entry>
	<!-- 数据库用户名 -->
	<entry key="db_user">root</entry>
	<!-- 数据库密码 -->
	<entry key="db_password">123456</entry>
	<!-- 同步间隔,单位:秒 -->
	<entry key="db_reload_interval">10</entry>
</properties>

```

> 自行打包 放入elasticsearch plugins 目录即可

启动日志如下:

```
[2021-06-02T15:16:07,593][INFO ][o.w.a.d.Dictionary       ] ======start mysql to reload ik dict.======
[2021-06-02T15:16:07,828][INFO ][o.w.a.d.Dictionary       ] last update mysql ext dic time :2021-05-27T14:36:05.000+0800,fill count:4843 ,disable count:0
[2021-06-02T15:16:07,837][INFO ][o.w.a.d.Dictionary       ] the last reload stop word not found, the last update time :null
[2021-06-02T15:16:07,838][INFO ][o.w.a.d.Dictionary       ] last update mysql stop word time :null,fill count:0 ,disable count:0
[2021-06-02T15:16:07,838][INFO ][o.w.a.d.Dictionary       ] ======reload mysql ik dict finished.======
[2021-06-02T15:16:17,587][INFO ][o.w.a.d.Dictionary       ] ======start mysql to reload ik dict.======
[2021-06-02T15:16:17,615][INFO ][o.w.a.d.Dictionary       ] last update mysql ext dic time :2021-06-01T09:44:50.000+0800,fill count:4842 ,disable count:0
[2021-06-02T15:16:17,623][INFO ][o.w.a.d.Dictionary       ] the last reload stop word not found, the last update time :null
[2021-06-02T15:16:17,624][INFO ][o.w.a.d.Dictionary       ] last update mysql stop word time :null,fill count:0 ,disable count:0
[2021-06-02T15:16:17,624][INFO ][o.w.a.d.Dictionary       ] ======reload mysql ik dict finished.======
[2021-06-02T15:16:27,596][INFO ][o.w.a.d.Dictionary       ] ======start mysql to reload ik dict.======
[2021-06-02T15:16:27,602][INFO ][o.w.a.d.Dictionary       ] the latest update record was not found, the last update time :2021-06-01T09:44:50.000+0800
[2021-06-02T15:16:27,602][INFO ][o.w.a.d.Dictionary       ] last update mysql ext dic time :2021-06-01T09:44:50.000+0800,fill count:0 ,disable count:0
[2021-06-02T15:16:27,608][INFO ][o.w.a.d.Dictionary       ] the last reload stop word not found, the last update time :null
[2021-06-02T15:16:27,608][INFO ][o.w.a.d.Dictionary       ] last update mysql stop word time :null,fill count:0 ,disable count:0
[2021-06-02T15:16:27,608][INFO ][o.w.a.d.Dictionary       ] ======reload mysql ik dict finished.======
```