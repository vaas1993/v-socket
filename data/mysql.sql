/*
 Navicat Premium Data Transfer

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 50739
 Source Host           : localhost:3306
 Source Schema         : v_socket

 Target Server Type    : MySQL
 Target Server Version : 50739
 File Encoding         : 65001

 Date: 21/02/2023 16:19:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for certificate
-- ----------------------------
DROP TABLE IF EXISTS `certificate`;
CREATE TABLE `certificate` (
  `appid` varchar(32) NOT NULL,
  `private` text NOT NULL,
  `public` text NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`appid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RSA证书';

SET FOREIGN_KEY_CHECKS = 1;
