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
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `domain` (`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
