package com.aisafe.common.enums;

/**
 * 操作日志业务类型
 */
public enum BusinessType {

    OTHER(0, "其它"),
    INSERT(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除"),
    REVIEW(4, "审核"),
    GRANT(5, "授权"),
    IMPORT(6, "导入");

    private final int code;
    private final String label;

    BusinessType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return OTHER.label;
        }
        for (BusinessType type : values()) {
            if (type.code == code) {
                return type.label;
            }
        }
        return OTHER.label;
    }
}
