/**
 * Copyright (C) 2019 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.storage.pathmapped;

import org.apache.commons.io.IOUtils;
import org.commonjava.storage.pathmapped.core.LayeredPhysicalStore;
import org.commonjava.storage.pathmapped.core.PathMappedFileManager;
import org.commonjava.storage.pathmapped.pathdb.datastax.CassandraPathDB;
import org.commonjava.storage.pathmapped.spi.StorageClassifier;
import org.commonjava.storage.pathmapped.util.PatternBasedStorageClassifier;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class StorageClassifierTest
                extends SimpleIOTest
{
    final String TEMP_1 = "temp-1";

    final String TEMP_2 = "temp-2";

    final String BUILD_1 = "build-1";

    @Override
    protected PathMappedFileManager initiateFileManager( File baseDir )
    {
        Map<Pattern, String> patternLevelMap = new HashMap<>();
        patternLevelMap.put( Pattern.compile( "^build-.+" ), "level-1" );
        patternLevelMap.put( Pattern.compile( "^temp-.+" ), "level-2" );

        StorageClassifier storageClassifier = new PatternBasedStorageClassifier( patternLevelMap );

        // Note: storageClassifier is passed to path DB, physical store, and fileManager
        LayeredPhysicalStore layeredPhysicalStore = new LayeredPhysicalStore( storageClassifier, baseDir );
        CassandraPathDB cassandraPathDB =
                        new CassandraPathDB( config, pathDB.getSession(), KEYSPACE, 1, storageClassifier );
        return new PathMappedFileManager( config, cassandraPathDB, layeredPhysicalStore, storageClassifier );
    }

    @Test
    public void sameLevelCopy() throws IOException
    {

        assertThat( fileManager.exists( TEMP_1, path1 ), equalTo( false ) );
        assertThat( fileManager.exists( TEMP_2, path2 ), equalTo( false ) );

        writeWithContent( fileManager.openOutputStream( TEMP_1, path1 ), simpleContent );
        fileManager.copy( TEMP_1, path1, TEMP_2, path2 );
        assertThat( fileManager.exists( TEMP_2, path2 ), equalTo( true ) );

        checkContent( TEMP_2, path2, simpleContent );

        assertEquals( fileManager.getFileStoragePath( TEMP_1, path1 ),
                      fileManager.getFileStoragePath( TEMP_2, path2 ) );

    }

    @Test
    public void differentLevelCopy() throws IOException
    {

        assertThat( fileManager.exists( TEMP_1, path1 ), equalTo( false ) );
        assertThat( fileManager.exists( BUILD_1, path2 ), equalTo( false ) );

        writeWithContent( fileManager.openOutputStream( TEMP_1, path1 ), simpleContent );
        fileManager.copy( TEMP_1, path1, BUILD_1, path2 );
        assertThat( fileManager.exists( BUILD_1, path2 ), equalTo( true ) );

        checkContent( BUILD_1, path2, simpleContent );

        assertNotEquals( fileManager.getFileStoragePath( TEMP_1, path1 ),
                         fileManager.getFileStoragePath( BUILD_1, path2 ) );

    }

    @Override
    public void read1MbFile() throws Exception
    {
        // ignore
    }

    @Override
    public void read11MbFile() throws Exception
    {
        // ignore
    }

    private void checkContent( String fs, String path, String expected ) throws IOException
    {
        try (InputStream is = fileManager.openInputStream( fs, path ))
        {
            Assert.assertNotNull( is );
            String result = new String( IOUtils.toByteArray( is ), Charset.defaultCharset() );
            assertThat( result, equalTo( expected ) );
        }
    }

    @Override
    protected void clearData()
    {
        super.clearData();
        fileManager.delete( TEMP_1, path1 );
        fileManager.delete( TEMP_2, path2 );
        fileManager.delete( BUILD_1, path2 );
    }

}
