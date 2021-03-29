package org.commonjava.storage.pathmapped.spi;

import java.util.Objects;

/**
 * This abstract class is to classify filesystems into different storage level. An storage classifier can be passed to
 * PathMappedFileManager, PathDB, or PhysicalStore (we exposes all the three classes to customer to get big flexibility).
 *
 * The storage level can be used to create cap dir for storageFile in physical store (e.g., LayeredPhysicalStore),
 * or give hints to pathDB to control de-dup (e.g, CassandraPathDB), and PathMappedFileManager to control copy behavior.
 *
 * Specifically, de-dup only occurs when the (filesystem, path) tuples are on same storage level. Copying to the same
 * level filesystem is db-only copy, and to different level filesystem will be hard copy (physical file copy).
 */
public abstract class StorageClassifier
{
    public static String DEFAULT_STORAGE_LEVEL = "level-default";

    /**
     * Get the storage level for the specified filesystem.
     * @return storage level, e.g., level-1, l2, etc.
     */
    public abstract String getStorageLevel( String fileSystem );

    public boolean isSameLevel( String fileSystem1, String fileSystem2 )
    {
        return Objects.equals( getStorageLevel( fileSystem1 ), getStorageLevel( fileSystem2 ) );
    }

    public boolean isDifferentLevel( String fileSystem1, String fileSystem2 )
    {
        return !isSameLevel( fileSystem1, fileSystem2 );
    }
}
