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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * IFT3913 tache 3 - vulnerabilite volontaire (injection SQL, CWE-89).
 * Recherche des documents deja extraits par titre dans une base de metadonnees.
 * Le titre (qui provient du document analyse) est concatene dans la requete.
 */
public class MetadataDbUtils {

    private MetadataDbUtils() {
    }

    public static ResultSet findByTitle(Connection connection, String title) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(
                "SELECT id, path FROM tika_metadata WHERE title = '" + title + "'");
    }
}
