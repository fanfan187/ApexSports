package com.fanfan.apexsports.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应包装类
 * <p>
 * 封装分页查询结果，包含记录列表、总条数、当前页码、每页大小和总页数
 * </p>
 *
 * @param <T> 记录类型
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long current;
    private long size;
    private long pages;

    public PageResult() {}

    /**
     * 构造分页响应
     *
     * @param records 记录列表
     * @param total   总条数
     * @param current 当前页码
     * @param size    每页大小
     * @param pages   总页数
     */
    public PageResult(List<T> records, long total, long current, long size, long pages) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = pages;
    }
}
