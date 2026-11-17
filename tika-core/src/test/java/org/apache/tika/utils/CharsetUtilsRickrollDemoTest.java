/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * IFT3913 tache 3 - test volontairement en echec, sur une branche jetable,
 * pour montrer que l'action rickroll se declenche quand un test echoue.
 */
public class CharsetUtilsRickrollDemoTest {

    @Test
    public void echoueVolontairementPourLeRickroll() {
        // CharsetUtils.clean("latin1") renvoie "ISO-8859-1" : l'assertion echoue.
        assertEquals("UTF-8", CharsetUtils.clean("latin1"));
    }
}
