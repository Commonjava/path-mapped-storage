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
package org.commonjava.storage.pathmapped.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

public class ChecksumCalculator
{
    private final MessageDigest digester;

    private String digestHex;

    public ChecksumCalculator( final String algorithm )
            throws NoSuchAlgorithmException
    {
        this.digester = MessageDigest.getInstance( algorithm );
    }

    public final void update( final byte[] data )
    {
        digester.update( data );
    }

    public final void update( final byte data )
    {
        digester.update( data );
    }

    public synchronized String getDigestHex()
    {
        if ( digestHex == null )
        {
            digestHex = encodeHexString( digester.digest() );
        }
        return digestHex;
    }
}
