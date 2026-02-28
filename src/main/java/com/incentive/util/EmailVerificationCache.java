package com.incentive.util;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class EmailVerificationCache {

    private record Entry(String code, LocalDateTime expiresAt) {}

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public void store(String email, String code) {
        cache.put(email.toLowerCase(), new Entry(code, LocalDateTime.now().plusMinutes(30)));
    }

    public boolean verify(String email, String code) {
        Entry entry = cache.get(email.toLowerCase());
        if (entry == null) return false;
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            cache.remove(email.toLowerCase());
            return false;
        }
        if (!entry.code().equals(code)) return false;
        cache.remove(email.toLowerCase());
        return true;
    }
}
