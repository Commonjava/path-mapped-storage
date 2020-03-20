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

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class QueryByPathTest
                extends AbstractCassandraFMTest
{
    @Test
    public void insertAndDelete() throws IOException
    {
        String repo1 = "maven:hosted:repo1";
        String repo2 = "maven:hosted:repo2";
        writeWithContent( fileManager.openOutputStream( repo1, path1 ), simpleContent );
        writeWithContent( fileManager.openOutputStream( repo2, path1 ), simpleContent );

        // repeat insert should not affect
        writeWithContent( fileManager.openOutputStream( repo1, path1 ), simpleContent );

        // insert two
        Set<String> ret = fileManager.getFileSystemContaining( path1 );
        System.out.println( ">>> " + ret );
        assertTrue( ret.size() == 2 );
        assertTrue( ret.containsAll( Arrays.asList( repo1, repo2 ) ) );

        // delete one
        fileManager.delete( repo1, path1 );
        ret = fileManager.getFileSystemContaining( path1 );
        System.out.println( ">>> " + ret );
        assertTrue( ret.size() == 1 );
        assertTrue( ret.contains( repo2 ) );
    }

    @Test
    public void copy() throws IOException
    {
        String from = "maven:hosted:repo1";
        String to = "maven:hosted:repo2";
        writeWithContent( fileManager.openOutputStream( from, path1 ), simpleContent );
        fileManager.copy( from, path1, to, path1 );

        Set<String> ret = fileManager.getFileSystemContaining( path1 );
        System.out.println( ">>> " + ret );
        assertTrue( ret.size() == 2 );
        assertTrue( ret.containsAll( Arrays.asList( from, to ) ) );
    }

}
