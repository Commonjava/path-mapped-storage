/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

public class ListingTest
        extends AbstractCassandraFMTest
{
    @Test
    public void listRootFolder()
            throws IOException
    {
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        List<String> lists = Arrays.asList( fileManager.list( TEST_FS, "/" ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( "root/" ) );
    }

    @Test
    public void listEntriesInSameFolder()
            throws IOException
    {
        List<String> lists = Arrays.asList( fileManager.list( TEST_FS, null ) );
        Assert.assertThat( lists.isEmpty(), CoreMatchers.equalTo( true ) );
        final String file3 = "target3.txt";
        final String path3 = pathSub1 + "/" + file3;
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        writeWithContent( fileManager.openOutputStream( TEST_FS, path3 ), simpleContent );
        lists = Arrays.asList( fileManager.list( TEST_FS, pathSub1 ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( file1, file3 ) );
        lists = Arrays.asList( fileManager.list( TEST_FS, pathParent ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( sub1 + "/" ) );
    }

    @Test
    public void listEntriesInDiffFolders()
            throws IOException
    {
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        writeWithContent( fileManager.openOutputStream( TEST_FS, path2 ), simpleContent );
        List<String> lists = Arrays.asList( fileManager.list( TEST_FS, pathParent ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( sub1 + "/", sub2 + "/" ) );

        lists = Arrays.asList( fileManager.list( TEST_FS, pathSub1 ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( file1 ) );
        lists = Arrays.asList( fileManager.list( TEST_FS, pathSub2 ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( file2 ) );
    }

    @Test
    public void listHugeNumOfEntries()
            throws IOException
    {
        int numOfFiles = 500;
        String[] files = new String[numOfFiles];
        for ( int i = 0; i < numOfFiles; i++ )
        {
            files[i] = "file" + i + ".txt";
            String filePath = pathSub1 + "/" + files[i];
            writeWithContent( fileManager.openOutputStream( TEST_FS, filePath ), simpleContent );
        }
        long start = System.currentTimeMillis();
        List<String> lists = Arrays.asList( fileManager.list( TEST_FS, pathSub1 ) );
        Assert.assertThat( lists, CoreMatchers.hasItems( files ) );
        long end = System.currentTimeMillis();
        logger.info( "Listing {} files took {} milliseconds", numOfFiles, end - start );
    }

    @Test
    public void listRecursively()
                    throws IOException
    {
        String[] paths = {
                        "/foo/1.0/foo-1.0.pom",
                        "/foo/1.0/foo-1.0.jar",
                        "/foo/bar/2.0/bar-2.0.pom",
                        "/foo/bar/2.0/bar-2.0.jar",
                        "/org/commonjava/util/weft/1.0/weft-1.0.pom",
                        "/org/commonjava/util/weft/2.0/weft-2.0.jar",
        };
        for ( String path : paths )
        {
            writeWithContent( fileManager.openOutputStream( TEST_FS, path ), simpleContent );
        }

        // list root
        List<String> lists = Arrays.asList( fileManager.list( TEST_FS, "/", true, 0 ) );
        lists.forEach( s -> System.out.println( s ) );
        for ( String path : paths )
        {
            Assert.assertThat( lists, CoreMatchers.hasItems( path.substring( 1 ) ) ); // remove heading /
        }

        // list /foo
        lists = Arrays.asList( fileManager.list( TEST_FS, "/foo", true, 0 ) );
        lists.forEach( s -> System.out.println( s ) );
        for ( String path : paths )
        {
            if ( path.startsWith( "/foo" ))
            {
                Assert.assertThat( lists, CoreMatchers.hasItems( path.substring( "/foo".length() + 1 ) ) ); // remove heading /foo/
            }
        }

        // list /foo/bar
        lists = Arrays.asList( fileManager.list( TEST_FS, "/foo/bar", true, 0 ) );
        lists.forEach( s -> System.out.println( s ) );
        for ( String path : paths )
        {
            if ( path.startsWith( "/foo/bar" ))
            {
                Assert.assertThat( lists, CoreMatchers.hasItems( path.substring( "/foo/bar".length() + 1 ) ) ); // remove heading /foo/bar/
            }
        }

        // list /foo/bar with limit 1 and expect exception
        try
        {
            fileManager.list( TEST_FS, "/foo/bar", true, 1 );
            fail("should not reach here due to exceeding limit exception");
        }
        catch ( Exception e )
        {
            System.out.println("Expected exception: " + e.getMessage());
        }
    }
}
