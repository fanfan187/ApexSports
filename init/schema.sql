-- =============================================
-- ApexSports 学生运动会管理系统 - 数据库架构
-- 版本: 1.0.0
-- =============================================

-- 设置字符集
SET NAMES utf8mb4;

-- =============================================
-- 1. 用户表 (as_user)
-- 存储管理员和工作人员账号信息
-- =============================================
DROP TABLE IF EXISTS `as_user`;
CREATE TABLE `as_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0-工作人员, 1-管理员',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 2. 学院表 (as_college)
-- =============================================
DROP TABLE IF EXISTS `as_college`;
CREATE TABLE `as_college` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学院ID',
    `name` VARCHAR(100) NOT NULL COMMENT '学院名称',
    `code` VARCHAR(20) NOT NULL COMMENT '学院代码',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '学院描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';

-- =============================================
-- 3. 运动员表 (as_athlete)
-- 每个运动员只能属于一个学院
-- =============================================
DROP TABLE IF EXISTS `as_athlete`;
CREATE TABLE `as_athlete` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运动员ID',
    `college_id` BIGINT NOT NULL COMMENT '所属学院ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `student_no` VARCHAR(30) NOT NULL COMMENT '学号',
    `gender` TINYINT NOT NULL COMMENT '性别: 0-女, 1-男',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁赛, 1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_college_id` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运动员表';

-- =============================================
-- 4. 比赛项目表 (as_event)
-- 区分个人项目和集体项目
-- =============================================
DROP TABLE IF EXISTS `as_event`;
CREATE TABLE `as_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `code` VARCHAR(20) NOT NULL COMMENT '项目代码',
    `type` TINYINT NOT NULL COMMENT '类型: 1-个人项目, 2-集体项目',
    `category` TINYINT NOT NULL COMMENT '类别: 1-径赛, 2-田赛',
    `gender` TINYINT NOT NULL COMMENT '性别限制: 0-女子, 1-男子, 2-混合',
    `unit` VARCHAR(20) NOT NULL COMMENT '成绩单位(秒/米/分等)',
    `max_participants` INT DEFAULT NULL COMMENT '集体项目最大参赛人数',
    `max_individual_events` INT NOT NULL DEFAULT 2 COMMENT '个人最多报名该项目次数',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type` (`type`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛项目表';

-- =============================================
-- 5. 裁判表 (as_referee)
-- =============================================
DROP TABLE IF EXISTS `as_referee`;
CREATE TABLE `as_referee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '裁判ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `gender` TINYINT NOT NULL COMMENT '性别: 0-女, 1-男',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `level` VARCHAR(30) DEFAULT NULL COMMENT '裁判等级',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-休息, 1-可用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='裁判表';

-- =============================================
-- 6. 场地表 (as_venue)
-- =============================================
DROP TABLE IF EXISTS `as_venue`;
CREATE TABLE `as_venue` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '场地ID',
    `name` VARCHAR(100) NOT NULL COMMENT '场地名称',
    `code` VARCHAR(20) NOT NULL COMMENT '场地代码',
    `type` TINYINT NOT NULL COMMENT '类型: 1-径赛场地, 2-田赛场地, 3-综合场地',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
    `capacity` INT DEFAULT NULL COMMENT '容纳人数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-不可用, 1-可用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地表';

-- =============================================
-- 7. 器材表 (as_equipment)
-- =============================================
DROP TABLE IF EXISTS `as_equipment`;
CREATE TABLE `as_equipment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '器材ID',
    `name` VARCHAR(100) NOT NULL COMMENT '器材名称',
    `code` VARCHAR(20) NOT NULL COMMENT '器材代码',
    `type` VARCHAR(50) DEFAULT NULL COMMENT '器材类型',
    `unit` VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '计量单位',
    `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '总数量',
    `available_quantity` INT NOT NULL DEFAULT 0 COMMENT '可用数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-可用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='器材表';

-- =============================================
-- 8. 比赛场次表 (as_competition)
-- 对应ER图中的schedule
-- =============================================
DROP TABLE IF EXISTS `as_competition`;
CREATE TABLE `as_competition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '比赛场次ID',
    `event_id` BIGINT NOT NULL COMMENT '项目ID',
    `venue_id` BIGINT NOT NULL COMMENT '场地ID',
    `round` TINYINT NOT NULL DEFAULT 1 COMMENT '轮次: 1-预赛, 2-决赛等',
    `group_no` TINYINT NOT NULL DEFAULT 1 COMMENT '组别',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛场次表';

-- =============================================
-- 9. 个人参赛记录表 (as_participation)
-- 对应ER图中的entry，运动员报名个人项目
-- 应用层约束：一人最多2项个人项目
-- =============================================
DROP TABLE IF EXISTS `as_participation`;
CREATE TABLE `as_participation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '参赛记录ID',
    `athlete_id` BIGINT NOT NULL COMMENT '运动员ID',
    `event_id` BIGINT NOT NULL COMMENT '项目ID',
    `competition_id` BIGINT DEFAULT NULL COMMENT '比赛场次ID',
    `lane_no` TINYINT DEFAULT NULL COMMENT '跑道号/道次',
    `bib_number` VARCHAR(20) DEFAULT NULL COMMENT '号码牌',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    -- 软删除后允许重报，故 uk 含 deleted 列
    UNIQUE KEY `uk_athlete_event` (`athlete_id`, `event_id`, `deleted`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_competition_id` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人参赛记录表';

-- =============================================
-- 10. 团体参赛记录表 (as_team_participation)
-- 对应ER图中的team_entry
-- =============================================
DROP TABLE IF EXISTS `as_team_participation`;
CREATE TABLE `as_team_participation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '团体参赛记录ID',
    `college_id` BIGINT NOT NULL COMMENT '学院ID',
    `event_id` BIGINT NOT NULL COMMENT '项目ID(集体项目)',
    `competition_id` BIGINT DEFAULT NULL COMMENT '比赛场次ID',
    `participant_count` INT NOT NULL DEFAULT 0 COMMENT '实际参赛人数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    -- 软删除后允许重报，故 uk 含 deleted 列
    UNIQUE KEY `uk_college_event` (`college_id`, `event_id`, `deleted`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_competition_id` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团体参赛记录表';

-- =============================================
-- 11. 裁判分配表 (as_referee_assignment)
-- 对应ER图中的assignment
-- 应用层约束：同一时间裁判只能主持一项比赛
-- =============================================
DROP TABLE IF EXISTS `as_referee_assignment`;
CREATE TABLE `as_referee_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '裁判分配ID',
    `competition_id` BIGINT NOT NULL COMMENT '比赛场次ID',
    `referee_id` BIGINT NOT NULL COMMENT '裁判ID',
    `role` TINYINT NOT NULL COMMENT '职务: 1-主裁判, 2-副裁判, 3-计时员, 4-记录员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-取消, 1-已分配',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_referee` (`competition_id`, `referee_id`),
    KEY `idx_referee_id` (`referee_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='裁判分配表';

-- =============================================
-- 12. 成绩表 (as_score)
-- 存储比赛成绩
-- =============================================
DROP TABLE IF EXISTS `as_score`;
CREATE TABLE `as_score` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
    `competition_id` BIGINT NOT NULL COMMENT '比赛场次ID',
    `participation_id` BIGINT DEFAULT NULL COMMENT '个人参赛记录ID',
    `team_participation_id` BIGINT DEFAULT NULL COMMENT '团体参赛记录ID',
    `athlete_id` BIGINT DEFAULT NULL COMMENT '运动员ID(个人项目)',
    `college_id` BIGINT DEFAULT NULL COMMENT '学院ID(团体项目)',
    `score` DECIMAL(10, 3) NOT NULL COMMENT '成绩值',
    `score_display` VARCHAR(50) NOT NULL COMMENT '成绩显示(如: 10.58秒)',
    `rank` TINYINT DEFAULT NULL COMMENT '排名',
    `points` INT DEFAULT NULL COMMENT '积分',
    `is_record_breaking` TINYINT NOT NULL DEFAULT 0 COMMENT '是否破纪录: 0-否, 1-是',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_competition_id` (`competition_id`),
    KEY `idx_participation_id` (`participation_id`),
    KEY `idx_team_participation_id` (`team_participation_id`),
    KEY `idx_athlete_id` (`athlete_id`),
    KEY `idx_college_id` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表';

-- =============================================
-- 13. 记录表 (as_record)
-- 存储各项比赛的历史记录
-- 应用层判断是否破纪录
-- =============================================
DROP TABLE IF EXISTS `as_record`;
CREATE TABLE `as_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `event_id` BIGINT NOT NULL COMMENT '项目ID',
    `record_type` TINYINT NOT NULL COMMENT '记录类型: 1-校记录, 2-市记录, 3-省记录',
    `athlete_id` BIGINT DEFAULT NULL COMMENT '运动员ID(个人项目)',
    `college_id` BIGINT DEFAULT NULL COMMENT '学院ID(团体项目)',
    `athlete_name` VARCHAR(50) DEFAULT NULL COMMENT '运动员姓名(冗余)',
    `college_name` VARCHAR(100) DEFAULT NULL COMMENT '学院名称(冗余)',
    `score` DECIMAL(10, 3) NOT NULL COMMENT '成绩值',
    `score_display` VARCHAR(50) NOT NULL COMMENT '成绩显示',
    `record_date` DATE NOT NULL COMMENT '创造记录日期',
    `competition_name` VARCHAR(200) DEFAULT NULL COMMENT '创造记录的比赛名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已作废, 1-有效',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录表';

-- =============================================
-- 14. 比赛器材使用表 (as_competition_equipment)
-- 记录每场比赛使用的器材
-- =============================================
DROP TABLE IF EXISTS `as_competition_equipment`;
CREATE TABLE `as_competition_equipment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '使用记录ID',
    `competition_id` BIGINT NOT NULL COMMENT '比赛场次ID',
    `equipment_id` BIGINT NOT NULL COMMENT '器材ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '使用数量',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-已分配, 1-使用中, 2-已归还',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_competition_id` (`competition_id`),
    KEY `idx_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛器材使用表';

-- =============================================
-- 15. 学院积分汇总表 (as_college_score_summary)
-- 定期汇总各学院的总积分
-- =============================================
DROP TABLE IF EXISTS `as_college_score_summary`;
CREATE TABLE `as_college_score_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '汇总ID',
    `college_id` BIGINT NOT NULL COMMENT '学院ID',
    `sports_meet_id` VARCHAR(50) DEFAULT NULL COMMENT '运动会ID/届次',
    `total_points` INT NOT NULL DEFAULT 0 COMMENT '总积分',
    `gold_count` INT NOT NULL DEFAULT 0 COMMENT '金牌数',
    `silver_count` INT NOT NULL DEFAULT 0 COMMENT '银牌数',
    `bronze_count` INT NOT NULL DEFAULT 0 COMMENT '铜牌数',
    `individual_points` INT NOT NULL DEFAULT 0 COMMENT '个人项目积分',
    `team_points` INT NOT NULL DEFAULT 0 COMMENT '团体项目积分',
    `rank` TINYINT DEFAULT NULL COMMENT '排名',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_college_sports_meet` (`college_id`, `sports_meet_id`),
    KEY `idx_total_points` (`total_points`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院积分汇总表';

-- =============================================
-- 16. 学院排名视图 (v_college_ranking)
-- 按总积分降序排列各学院排名
-- =============================================
DROP VIEW IF EXISTS `v_college_ranking`;
CREATE VIEW `v_college_ranking` AS
SELECT
    css.id,
    css.college_id,
    c.name AS college_name,
    c.code AS college_code,
    css.sports_meet_id,
    css.total_points,
    css.gold_count,
    css.silver_count,
    css.bronze_count,
    css.individual_points,
    css.team_points,
    RANK() OVER (ORDER BY css.total_points DESC) AS ranking
FROM as_college_score_summary css
JOIN as_college c ON css.college_id = c.id
WHERE css.deleted = 0 AND c.deleted = 0
ORDER BY css.total_points DESC;
