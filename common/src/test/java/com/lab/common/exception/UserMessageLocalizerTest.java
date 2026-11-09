package com.lab.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMessageLocalizerTest {
    @Test
    void knownCodeReturnsLocalizedMessage() {
        assertThat(UserMessageLocalizer.resolve("LOGIN_FAILED", "invalid credentials"))
                .isEqualTo("账号或密码错误");
    }

    @Test
    void unknownCodeWithChineseOriginalKeepsOriginal() {
        assertThat(UserMessageLocalizer.resolve("UNKNOWN_CODE", "请检查填写内容"))
                .isEqualTo("请检查填写内容");
    }

    @Test
    void unknownCodeWithEnglishOriginalUsesFallback() {
        assertThat(UserMessageLocalizer.resolve("UNKNOWN_CODE", "bad request"))
                .isEqualTo("操作未完成，请稍后重试");
    }

    @Test
    void unknownCodeWithNullOriginalUsesFallback() {
        assertThat(UserMessageLocalizer.resolve("UNKNOWN_CODE", null))
                .isEqualTo("操作未完成，请稍后重试");
    }
}
