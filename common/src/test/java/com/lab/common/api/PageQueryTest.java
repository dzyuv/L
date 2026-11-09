package com.lab.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {
    @Test
    void defaultsInvalidValues() {
        PageQuery query = PageQuery.of(null, null);

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
    }

    @Test
    void clampsPageAndSize() {
        PageQuery query = PageQuery.of(0, 5000);

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(PageQuery.MAX_SIZE);
    }
}
