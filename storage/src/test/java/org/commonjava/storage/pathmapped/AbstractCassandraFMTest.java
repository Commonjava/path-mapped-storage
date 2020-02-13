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

import com.datastax.driver.core.Session;
import org.apache.commons.io.IOUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.storage.pathmapped.config.DefaultPathMappedStorageConfig;
import org.commonjava.storage.pathmapped.core.FileBasedPhysicalStore;
import org.commonjava.storage.pathmapped.core.PathMappedFileManager;
import org.commonjava.storage.pathmapped.pathdb.datastax.CassandraPathDB;
import org.commonjava.storage.pathmapped.spi.FileInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_HOST;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_KEYSPACE;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_PORT;

public abstract class AbstractCassandraFMTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    static final long GC_WAIT_MS = 300;

    static final int COUNT = 2000;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public TestName name = new TestName();

    private static CassandraPathDB pathDB;

    PathMappedFileManager fileManager;

    static final String KEYSPACE = "test";

    static final String TEST_FS = "test";

    private String baseStoragePath;

    private static DefaultPathMappedStorageConfig config;

    private final String root = "/root";

    final String parent = "parent";

    final String pathParent = root + "/" + parent;

    final String sub1 = "sub1";

    final String sub2 = "sub2";

    final String pathSub1 = pathParent + "/" + sub1;

    final String pathSub2 = pathParent + "/" + sub2;

    final String file1 = "target1.txt";

    final String file2 = "target2.txt";

    final String path1 = pathSub1 + "/" + file1;

    final String path2 = pathSub2 + "/" + file2;

    final String simpleContent = "This is a test";

    @BeforeClass
    public static void startEmbeddedCassandra()
            throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        Map<String, Object> props = new HashMap<>();
        props.put( PROP_CASSANDRA_HOST, "localhost" );
        props.put( PROP_CASSANDRA_PORT, 9142 );
        props.put( PROP_CASSANDRA_KEYSPACE, KEYSPACE );

        config = new DefaultPathMappedStorageConfig( props );
        // In test, we should let gc happened immediately when triggered.
        config.setGcGracePeriodInHours( 0 );
        config.setDeduplicatePattern( "^(generic|npm|test).*" );
        pathDB = new CassandraPathDB( config );

    }

    @AfterClass
    public static void shutdown()
    {
        if ( pathDB != null )
        {
            pathDB.close();
        }
    }

    String getBaseDir()
    {
        return baseStoragePath;
    }

    @Before
    public void setup()
            throws Exception
    {
        File baseDir = temp.newFolder();
        baseStoragePath = baseDir.getCanonicalPath();
        fileManager = new PathMappedFileManager( config, pathDB,
                                                 new FileBasedPhysicalStore( baseDir ) );
    }

    @After
    public void teardown()
    {
        clearData();
        clearCommon();
        cleanAllData();
    }

    private void cleanAllData()
    {
        if(pathDB!=null){
            Session session = pathDB.getSession();
            session.execute( "TRUNCATE " + KEYSPACE + ".pathmap;" );
            session.execute( "TRUNCATE " + KEYSPACE + ".reversemap;" );
            session.execute( "TRUNCATE " + KEYSPACE + ".reclaim;" );
            session.execute( "TRUNCATE " + KEYSPACE + ".filechecksum;" );
        }
    }

    private void clearCommon(){
        fileManager.delete( TEST_FS, path1 );
        fileManager.delete( TEST_FS, path2 );
        for ( Map.Entry<FileInfo, Boolean> entry : fileManager.gc().entrySet() )
        {
            logger.info( "{} has been swept by gc", entry.getKey() );
        }
        try
        {
            sleep( GC_WAIT_MS );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    void writeWithContent( OutputStream stream, String content )
    {
        try (OutputStream os = stream)
        {
            IOUtils.write( content.getBytes(), os );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    void writeWithContent( String fileSystem, String path, String content )
    {
        try
        {
            writeWithContent( fileManager.openOutputStream( fileSystem, path ), content );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    protected void clearData()
    {
    }
}
