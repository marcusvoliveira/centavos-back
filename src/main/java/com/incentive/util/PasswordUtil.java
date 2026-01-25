package com.incentive.util;

import io.quarkus.elytron.security.common.BcryptUtil;

public class PasswordUtil {

    public static String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        return BcryptUtil.matches(password, hashedPassword);
    }
}
