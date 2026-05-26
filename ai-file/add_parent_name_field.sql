-- 为 biz_tag_category 表添加父级节点名称字段
-- 注意：description 字段已经存在

ALTER TABLE biz_tag_category 
ADD COLUMN parent_name VARCHAR(255) COMMENT '父级节点名称' 
AFTER description;
