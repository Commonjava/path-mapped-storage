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
package org.commonjava.storage.pathmapped.pathdb.jpa.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.commonjava.storage.pathmapped.model.ProxySite;

import java.util.Objects;

@Entity
@Table( name = "proxysites" )
public class JpaProxySite
        implements ProxySite
{
    @Id
    private String site;

    public JpaProxySite()
    {
    }

    public JpaProxySite( String site )
    {
        this.site = site;
    }

    @Override
    public String getSite()
    {
        return site;
    }

    public void setSite( String site )
    {
        this.site = site;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        JpaProxySite that = (JpaProxySite) o;
        return site.equals( that.site );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( site );
    }

    @Override
    public String toString()
    {
        return "JpaProxySite{" + "site='" + site + '}';
    }
}
