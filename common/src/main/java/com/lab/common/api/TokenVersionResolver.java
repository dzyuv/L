package com.lab.common.api;

/** Resolves the current token version for a user so access tokens can be revoked immediately. */
public interface TokenVersionResolver {
    /**
     * @return the current version, a negative sentinel for a disabled/unknown user,
     *         or {@code null} when the version cannot be resolved (treated as fail-open).
     */
    Integer currentVersion(Long userId);
}
