IK Analysis for Elasticsearch
=============================

The IK Analysis plugin integrates Lucene IK analyzer (http://code.google.com/p/ik-analyzer/) into elasticsearch, support customized dictionary.

Analyzer: `ik_smart` , `ik_max_word` , Tokenizer: `ik_smart` , `ik_max_word`

### 2021.7.12更新 by Qicz

- 改造使用yml配置文件；

```yaml
dict: # 扩展词库配置
  local: # 本地扩展词典配置
    main: # 本地主词典扩展词典文件
      - extra_main.dic
      - extra_single_word.dic
      - extra_single_word_full.dic
      - extra_single_word_low_freq.dic
    stop: # 本地stop词典扩展词典文件
      - extra_stopword.dic
  remote: # 远程扩展词典配置
    http:
      # http 服务地址
      # main-words path: ${base}/es-dict/main-words/{domain}
      # stop-words path: ${base}/es-dict/stop-words/{domain}
      base: http://localhost
    redis:
      # main-words key: es-ik-words:{domain}:main-words
      # stop-words key: es-ik-words:{domain}:stop-words
      host: localhost
      port: 6379
      database: 0
      username:
      password:
    mysql:
      url: jdbc:mysql://127.0.0.1/ik-db?useSSL=false&serverTimezone=GMT%2B8
      username: root
      password: dbadmin
    refresh: # 刷新配置
      delay: 10 # 延迟时间，单位s
      period: 60 # 周期时间，单位s
```

- 调整优化重构Dictionary实现；
- 支持根据不同的业务指定远程动态词源

```bash
PUT es-ik-index
{
    "settings": {
        "analysis.analyzer": {
            "ik_smart": {
                "type": "ik_smart",
                "enable_remote_dict": true,
                "domain": "order", # 业务领域
                "etymology": "redis" # 词源，可取值：redis，http，mysql，默认为redis
            }
        }
    },
    "mappings": {
        "properties": {
            "field": {
                "type": "text",
                "analyzer": "ik_smart"
            }
        }
    }
}
```



- 修复和重构Http扩展词提供方式的bug；
- 扩展RemoteDictionary，提供可配置的基于MySQL、Redis的扩展词库更新方式；

```sql
/*
 @author Qicz

 Date: 13/07/2021 10:18:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ik_dict_state
-- ----------------------------
DROP TABLE IF EXISTS `ik_dict_state`;
CREATE TABLE `ik_dict_state` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  `state` varchar(10) NOT NULL COMMENT 'newly有更新non-newly无更新',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `domain` (`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ik_words
-- ----------------------------
DROP TABLE IF EXISTS `ik_words`;
CREATE TABLE `ik_words` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(200) NOT NULL,
  `word_type` tinyint(4) unsigned NOT NULL COMMENT 'word类型，1主词库，2stop词库',
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `domain` (`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
```

> `jre/lib/security/java.policy`的grant中加入 `permission java.security.AllPermission;`

- 【TODO】 整理一个完成的使用手册
- [基于SpringBoot的MySQL、Redis扩展词库写入starter](https://github.com/OpeningO/redip)

Versions
--------

IK version | ES version
-----------|-----------
master | 7.x -> master
6.x| 6.x
5.x| 5.x
1.10.6 | 2.4.6
1.9.5 | 2.3.5
1.8.1 | 2.2.1
1.7.0 | 2.1.1
1.5.0 | 2.0.0
1.2.6 | 1.0.0
1.2.5 | 0.90.x
1.1.3 | 0.20.x
1.0.0 | 0.16.2 -> 0.19.0

Install
-------

1.download or compile

* optional 1 - download pre-build package from here: https://github.com/medcl/elasticsearch-analysis-ik/releases

    create plugin folder `cd your-es-root/plugins/ && mkdir ik`
    
    unzip plugin to folder `your-es-root/plugins/ik`

* optional 2 - use elasticsearch-plugin to install ( supported from version v5.5.1 ):

    ```
    ./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.3.0/elasticsearch-analysis-ik-6.3.0.zip
    ```

   NOTE: replace `6.3.0` to your own elasticsearch version

2.restart elasticsearch



#### Quick Example

1.create a index

```bash
curl -XPUT http://localhost:9200/index
```

2.create a mapping

```bash
curl -XPOST http://localhost:9200/index/_mapping -H 'Content-Type:application/json' -d'
{
        "properties": {
            "content": {
                "type": "text",
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart"
            }
        }

}'
```

3.index some docs

```bash
curl -XPOST http://localhost:9200/index/_create/1 -H 'Content-Type:application/json' -d'
{"content":"美国留给伊拉克的是个烂摊子吗"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/2 -H 'Content-Type:application/json' -d'
{"content":"公安部：各地校车将享最高路权"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/3 -H 'Content-Type:application/json' -d'
{"content":"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/4 -H 'Content-Type:application/json' -d'
{"content":"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"}
'
```

4.query with highlighting

```bash
curl -XPOST http://localhost:9200/index/_search  -H 'Content-Type:application/json' -d'
{
    "query" : { "match" : { "content" : "中国" }},
    "highlight" : {
        "pre_tags" : ["<tag1>", "<tag2>"],
        "post_tags" : ["</tag1>", "</tag2>"],
        "fields" : {
            "content" : {}
        }
    }
}
'
```

Result

```json
{
    "took": 14,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 2,
        "hits": [
            {
                "_index": "index",
                "_type": "fulltext",
                "_id": "4",
                "_score": 2,
                "_source": {
                    "content": "中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"
                },
                "highlight": {
                    "content": [
                        "<tag1>中国</tag1>驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首 "
                    ]
                }
            },
            {
                "_index": "index",
                "_type": "fulltext",
                "_id": "3",
                "_score": 2,
                "_source": {
                    "content": "中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"
                },
                "highlight": {
                    "content": [
                        "均每天扣1艘<tag1>中国</tag1>渔船 "
                    ]
                }
            }
        ]
    }
}
```

### Dictionary Configuration

`IKAnalyzer.cfg.xml` can be located at `{conf}/analysis-ik/config/IKAnalyzer.cfg.xml`
or `{plugins}/elasticsearch-analysis-ik-*/config/IKAnalyzer.cfg.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<comment>IK Analyzer 扩展配置</comment>
	<!--用户可以在这里配置自己的扩展字典 -->
	<entry key="ext_dict">custom/mydict.dic;custom/single_word_low_freq.dic</entry>
	 <!--用户可以在这里配置自己的扩展停止词字典-->
	<entry key="ext_stopwords">custom/ext_stopword.dic</entry>
 	<!--用户可以在这里配置远程扩展字典 -->
	<entry key="remote_ext_dict">location</entry>
 	<!--用户可以在这里配置远程扩展停止词字典-->
	<entry key="remote_ext_stopwords">http://xxx.com/xxx.dic</entry>
</properties>
```

### 热更新 IK 分词使用方法

目前该插件支持热更新 IK 分词，通过上文在 IK 配置文件中提到的如下配置

```xml
 	<!--用户可以在这里配置远程扩展字典 -->
	<entry key="remote_ext_dict">location</entry>
 	<!--用户可以在这里配置远程扩展停止词字典-->
	<entry key="remote_ext_stopwords">location</entry>
```

其中 `location` 是指一个 url，比如 `http://yoursite.com/getCustomDict`，该请求只需满足以下两点即可完成分词热更新。

1. 该 http 请求需要返回两个头部(header)，一个是 `Last-Modified`，一个是 `ETag`，这两者都是字符串类型，只要有一个发生变化，该插件就会去抓取新的分词进而更新词库。

2. 该 http 请求返回的内容格式是一行一个分词，换行符用 `\n` 即可。

满足上面两点要求就可以实现热更新分词了，不需要重启 ES 实例。

可以将需自动更新的热词放在一个 UTF-8 编码的 .txt 文件里，放在 nginx 或其他简易 http server 下，当 .txt 文件修改时，http server 会在客户端请求该文件时自动返回相应的 Last-Modified 和 ETag。可以另外做一个工具来从业务系统提取相关词汇，并更新这个 .txt 文件。

have fun.

常见问题
-------

1.自定义词典为什么没有生效？

请确保你的扩展词典的文本格式为 UTF8 编码

2.如何手动安装？


```bash
git clone https://github.com/medcl/elasticsearch-analysis-ik
cd elasticsearch-analysis-ik
git checkout tags/{version}
mvn clean
mvn compile
mvn package
```

拷贝和解压release下的文件: #{project_path}/elasticsearch-analysis-ik/target/releases/elasticsearch-analysis-ik-*.zip 到你的 elasticsearch 插件目录, 如: plugins/ik
重启elasticsearch

3.分词测试失败
请在某个索引下调用analyze接口测试,而不是直接调用analyze接口
如:
```bash
curl -XGET "http://localhost:9200/your_index/_analyze" -H 'Content-Type: application/json' -d'
{
   "text":"中华人民共和国MN","tokenizer": "my_ik"
}'
```


4. ik_max_word 和 ik_smart 什么区别?


ik_max_word: 会将文本做最细粒度的拆分，比如会将“中华人民共和国国歌”拆分为“中华人民共和国,中华人民,中华,华人,人民共和国,人民,人,民,共和国,共和,和,国国,国歌”，会穷尽各种可能的组合，适合 Term Query；

ik_smart: 会做最粗粒度的拆分，比如会将“中华人民共和国国歌”拆分为“中华人民共和国,国歌”，适合 Phrase 查询。

Changes
------
*自 v5.0.0 起*

- 移除名为 `ik` 的analyzer和tokenizer,请分别使用 `ik_smart` 和 `ik_max_word`


Thanks
------
YourKit supports IK Analysis for ElasticSearch project with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
