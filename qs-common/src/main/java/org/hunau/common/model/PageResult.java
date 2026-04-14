package org.hunau.common.model;

import lombok.Data;
import org.hunau.common.constant.CommonConstants;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }
    
    public static <T> PageResult<T> empty() {
        return of(List.of(), 0L, 1, CommonConstants.DEFAULT_PAGE_SIZE);
    }
}