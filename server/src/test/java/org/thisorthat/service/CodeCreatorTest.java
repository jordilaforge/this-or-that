/*
 * Copyright 2018 Baloise Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thisorthat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeCreatorTest {

    @Test
    @DisplayName("Should create minimum 4-character code for index 0")
    void testCreateCodeForZero() {
        String code = CodeCreator.createCode(0);
        assertNotNull(code);
        assertTrue(code.length() >= 4);
    }

    @Test
    @DisplayName("Should be deterministic for the same index")
    void testDeterministicCode() {
        String code1 = CodeCreator.createCode(42);
        String code2 = CodeCreator.createCode(42);
        assertEquals(code1, code2);
    }

    @Test
    @DisplayName("Should generate distinct codes for different indices")
    void testDistinctCodes() {
        String codeA = CodeCreator.createCode(1);
        String codeB = CodeCreator.createCode(2);
        assertNotEquals(codeA, codeB);
    }

    @Test
    @DisplayName("Code contains only alphanumeric characters from custom alphabet")
    void testCodeCharacters() {
        String code = CodeCreator.createCode(999);
        assertTrue(code.matches("^[a-z0-9]+$"));
    }
}
