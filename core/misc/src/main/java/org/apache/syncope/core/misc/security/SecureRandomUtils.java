/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.misc.security;

import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;

public final class SecureRandomUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomPassword(final int tokenLength) {
        return RandomStringUtils.random(tokenLength, 0, 0, true, false, null, RANDOM);
    }

    public static String generateRandomLetter() {
        return RandomStringUtils.random(1, 0, 0, true, false, null, RANDOM);
    }

    public static String generateRandomNumber() {
        return RandomStringUtils.random(1, 0, 0, false, true, null, RANDOM);
    }

    public static String generateRandomSpecialCharacter(final char[] characters) {
        return RandomStringUtils.random(1, 0, 0, false, false, characters, RANDOM);
    }

    private SecureRandomUtils() {
        // private constructor for static utility class
    }
}
