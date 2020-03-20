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
package org.commonjava.storage.pathmapped.pathdb.datastax.model;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;

@Table( name = "path", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxPath
{
    @PartitionKey
    private String path;

    @Column
    private HashSet<String> fileSystems;

    @Column
    private Date lastModify;

    public DtxPath()
    {
    }

    public DtxPath( String path, HashSet<String> fileSystems, Date lastModify )
    {
        this.path = path;
        this.fileSystems = fileSystems;
        this.lastModify = lastModify;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        DtxPath dtxPath = (DtxPath) o;
        return path.equals( dtxPath.path );
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public HashSet<String> getFileSystems()
    {
        return fileSystems;
    }

    public void setFileSystems( HashSet<String> fileSystems )
    {
        this.fileSystems = fileSystems;
    }

    public Date getLastModify()
    {
        return lastModify;
    }

    public void setLastModify( Date lastModify )
    {
        this.lastModify = lastModify;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( path );
    }

    @Override
    public String toString()
    {
        return "DtxPath{" + "path='" + path + '\'' + ", fileSystems=" + fileSystems + ", lastModify=" + lastModify
                        + '}';
    }
}
