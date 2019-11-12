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

import ch.qos.logback.core.util.FileSize;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.storage.pathmapped.spi.FileInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class SimpleIOTest
        extends AbstractCassandraFMTest
{
    private final String TEMP_FS = "TEMP_FS";

    @Test
    public void readWrittenFile()
            throws Exception
    {
        try (OutputStream os = fileManager.openOutputStream( TEST_FS, path1 ))
        {
            Assert.assertNotNull( os );
            IOUtils.write( simpleContent.getBytes(), os );
        }

        try (InputStream is = fileManager.openInputStream( TEST_FS, path1 ))
        {
            Assert.assertNotNull( is );
            String result = new String( IOUtils.toByteArray( is ), Charset.defaultCharset() );
            Assert.assertThat( result, CoreMatchers.equalTo( simpleContent ) );
        }
    }

    @Test
    public void getRealFile()
            throws Exception
    {
        readWrittenFile();
        String realFilePath = fileManager.getFileStoragePath( TEST_FS, path1 );
        Assert.assertThat( realFilePath, CoreMatchers.notNullValue() );
        Assert.assertThat( FileUtils.readFileToString( Paths.get( getBaseDir(), realFilePath ).toFile() ),
                           CoreMatchers.equalTo( simpleContent ) );
    }

    @Test
    public void getFileMetadata()
            throws Exception
    {
        Assert.assertThat( fileManager.getFileLength( TEST_FS, null ), CoreMatchers.equalTo( 0L ) );
        Assert.assertThat( fileManager.getFileLastModified( TEST_FS, null ), CoreMatchers.equalTo( -1L ) );
        readWrittenFile();
        Assert.assertThat( fileManager.getFileLength( TEST_FS, path1 ), CoreMatchers.equalTo( (long) simpleContent.length() ) );
        Assert.assertThat( fileManager.getFileLastModified( TEST_FS, path1 ) > 0, CoreMatchers.equalTo( true ) );
    }

    @Test
    public void read1MbFile()
            throws Exception
    {
        assertReadFileOfSize( "1mb" );
    }

    @Test
    public void read11MbFile()
            throws Exception
    {
        assertReadFileOfSize( "11mb" );
    }

    private void assertReadFileOfSize( String s )
            throws IOException
    {
        int sz = (int) FileSize.valueOf( s ).getSize();
        byte[] src = new byte[sz];

        Random rand = new Random();
        rand.nextBytes( src );

        try (OutputStream out = fileManager.openOutputStream( TEST_FS, path1 ))
        {
            IOUtils.write( src, out );
        }

        try (InputStream stream = fileManager.openInputStream( TEST_FS, path1 ))
        {
            byte[] result = IOUtils.toByteArray( stream );
            Assert.assertThat( result, CoreMatchers.equalTo( src ) );
        }
    }

    @Test
    public void writeToFileWithLines()
            throws Exception
    {

        writeWithCount( fileManager.openOutputStream( TEST_FS, path1 ) );

        final File file = new File( path1 );
        try (InputStream is = fileManager.openInputStream( TEST_FS, path1 ))
        {
            final List<String> lines = IOUtils.readLines( is );
            Assert.assertThat( lines.size(), CoreMatchers.equalTo( COUNT ) );
        }

    }

    @Test
    public void overwriteFile()
            throws Exception
    {
        try (OutputStream stream = fileManager.openOutputStream( TEST_FS, path1 ))
        {
            String longer = "This is a really really really long string";
            stream.write( longer.getBytes() );
        }

        String shorter = "This is a short string";
        try (OutputStream stream = fileManager.openOutputStream( TEST_FS, path1 ))
        {
            stream.write( shorter.getBytes() );
        }

        long fileLength = fileManager.getFileLength( TEST_FS, path1 );
        Assert.assertThat( fileLength, CoreMatchers.equalTo( (long) shorter.getBytes().length ) );

        try (InputStream stream = fileManager.openInputStream( TEST_FS, path1 ))
        {
            String content = IOUtils.toString( stream );
            Assert.assertThat( content, CoreMatchers.equalTo( shorter ) );
        }

    }

    @Test
    public void fileRepeatedRead()
            throws Exception
    {
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        InputStream s1 = null;
        InputStream s2 = null;

        try
        {

            s1 = fileManager.openInputStream( TEST_FS, path1 );
            s2 = fileManager.openInputStream( TEST_FS, path1 );

            logger.info( "READ first " );
            String out1 = IOUtils.toString( s1 );
            logger.info( "READ second " );
            String out2 = IOUtils.toString( s2 );

            Assert.assertThat( "first reader returned wrong data", out1, CoreMatchers.equalTo( simpleContent ) );
            Assert.assertThat( "second reader returned wrong data", out2, CoreMatchers.equalTo( simpleContent ) );
        }
        finally
        {
            logger.info( "CLOSE first thread" );
            IOUtils.closeQuietly( s1 );
            logger.info( "CLOSE second thread" );
            IOUtils.closeQuietly( s2 );
        }

    }

    @Test
    public void delete()
            throws IOException
    {
        try (InputStream is = fileManager.openInputStream( TEST_FS, path1 ))
        {
            Assert.assertThat( is, CoreMatchers.nullValue() );
        }
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        try (InputStream is = fileManager.openInputStream( TEST_FS, path1 ))
        {
            Assert.assertThat( is, CoreMatchers.notNullValue() );
        }
        Assert.assertThat( fileManager.delete( TEST_FS, path1 ), CoreMatchers.equalTo( true ) );
        try (InputStream is = fileManager.openInputStream( TEST_FS, path1 ))
        {
            Assert.assertThat( is, CoreMatchers.nullValue() );
        }
        //NOTE: not allow to delete a folder
        Assert.assertThat( fileManager.delete( TEST_FS, pathSub1 + "/" ), CoreMatchers.equalTo( false ) );
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, pathSub1, true );
    }

    @Test
    public void existsFileOrDir()
            throws IOException
    {
        Assert.assertThat( fileManager.exists( TEST_FS, null ), CoreMatchers.equalTo( false ) );
        final String tempRoot = "/temp-exists";
        final String tempPathParent = tempRoot + "/" + parent;
        final String tempPath = tempPathParent + "/" + file1;
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, tempPath, false );
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, tempPathParent, false );
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, tempRoot, false );
        writeWithContent( fileManager.openOutputStream( TEST_FS, tempPath ), simpleContent );
        Assert.assertThat( fileManager.exists( TEST_FS, tempPath ), CoreMatchers.equalTo( true ) );
        Assert.assertThat( fileManager.exists( TEST_FS, tempPath + "/" ), CoreMatchers.equalTo( false ) );
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, tempPathParent, true );
        assertPathWithChecker( ( f, p ) -> fileManager.exists( f, p ), TEST_FS, tempRoot, true );
    }

    @Test
    public void isFileOrDir()
            throws IOException
    {
        Assert.assertThat( fileManager.isFile( TEST_FS, null ), CoreMatchers.equalTo( false ) );
        Assert.assertThat( fileManager.isDirectory( TEST_FS, null ), CoreMatchers.equalTo( false ) );
        final String tempRoot = "/temp-dir";
        final String tempPathParent = tempRoot + "/" + parent;
        final String tempPath = tempPathParent + "/" + file1;
        Assert.assertThat( fileManager.isFile( TEST_FS, path1 ), CoreMatchers.equalTo( false ) );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPath, false );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathParent, false );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempRoot, false );
        writeWithContent( fileManager.openOutputStream( TEST_FS, tempPath ), simpleContent );
        Assert.assertThat( fileManager.isFile( TEST_FS, tempPath ), CoreMatchers.equalTo( true ) );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPath, false );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathParent, true );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempRoot, true );
    }

    @Test
    public void makeDirs()
    {
        final String tempRoot = "/temp-dirs";
        final String tempPathParent = tempRoot + "/" + parent;
        final String tempPathSub = tempPathParent + "/" + sub1;
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempRoot, false );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathParent, false );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathSub, false );
        fileManager.makeDirs( TEST_FS, tempPathSub );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempRoot, true );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathParent, true );
        assertPathWithChecker( ( f, p ) -> fileManager.isDirectory( f, p ), TEST_FS, tempPathSub, true );
    }

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
    public void simpleCopy()
            throws IOException
    {
        Assert.assertThat( fileManager.exists( TEST_FS, path1 ), CoreMatchers.equalTo( false ) );
        Assert.assertThat( fileManager.exists( TEMP_FS, path2 ), CoreMatchers.equalTo( false ) );
        writeWithContent( fileManager.openOutputStream( TEST_FS, path1 ), simpleContent );
        fileManager.copy( TEST_FS, path1, TEMP_FS, path2 );
        Assert.assertThat( fileManager.exists( TEMP_FS, path2 ), CoreMatchers.equalTo( true ) );
        try (InputStream is = fileManager.openInputStream( TEMP_FS, path2 ))
        {
            Assert.assertNotNull( is );
            String result = new String( IOUtils.toByteArray( is ), Charset.defaultCharset() );
            Assert.assertThat( result, CoreMatchers.equalTo( simpleContent ) );
        }
    }

    @Test
    public void gc()
            throws Exception
    {
        readWrittenFile();
        File realFile = Paths.get( getBaseDir(), fileManager.getFileStoragePath( TEST_FS, path1 ) ).toFile();
        Assert.assertThat( realFile.exists(), CoreMatchers.equalTo( true ) );
        Assert.assertThat( FileUtils.readFileToString( realFile ), CoreMatchers.equalTo( simpleContent ) );
        fileManager.delete( TEST_FS, path1 );
        sleep( GC_WAIT_MS ); // so that gc is sure to removes it
        Map<FileInfo, Boolean> ret = fileManager.gc();
        logger.info( "GC result: {}", ret );
        Assert.assertThat( realFile.exists(), CoreMatchers.equalTo( false ) );
    }

    private void writeWithCount( OutputStream stream )
    {
        try (OutputStream os = stream)
        {
            for ( int i = 0; i < COUNT; i++ )
            {
                os.write( String.format( "%d\n", i ).getBytes() );
            }

        }
        catch ( final IOException e )
        {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface PathChecker<T>
    {
        T checkPath( String fileSystem, String path );
    }

    private void assertPathWithChecker( PathChecker<Boolean> checker, String fileSystem, String path, boolean expected )
    {
        Assert.assertThat( checker.checkPath( fileSystem, path ), CoreMatchers.equalTo( expected ) );
        //        assertThat( checker.checkPath( fileSystem, path + "/" ), equalTo( expected ) );
    }

    @Override
    protected void clearData()
    {
        super.clearData();
        fileManager.delete( TEMP_FS, path1 );
        fileManager.delete( TEMP_FS, path2 );
    }
}
