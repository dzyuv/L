package com.lab.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public record PageQuery(int page, int size) {
    public static final int DEFAULT_SIZE = 1000;
    public static final int MAX_SIZE = 1000;

    public static PageQuery of(Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageQuery(current, pageSize);
    }

    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}
