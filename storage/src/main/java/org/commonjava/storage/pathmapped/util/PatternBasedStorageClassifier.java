package org.commonjava.storage.pathmapped.util;

import org.commonjava.storage.pathmapped.spi.StorageClassifier;

import java.util.Map;
import java.util.regex.Pattern;

public class PatternBasedStorageClassifier extends StorageClassifier
{
    // pattern -> storageLevel, e.g, "^build-.+" -> level-1, "^temp.+" -> level-2, etc
    private final Map<Pattern, String> patternLevelMap;

    public PatternBasedStorageClassifier( Map<Pattern, String> patternLevelMap )
    {
        this.patternLevelMap = patternLevelMap;
    }

    public String getStorageLevel( String fileSystem )
    {
        for ( Map.Entry<Pattern, String> entry : patternLevelMap.entrySet() )
        {
            if ( entry.getKey().matcher( fileSystem ).matches() )
            {
                return entry.getValue();
            }
        }
        return DEFAULT_STORAGE_LEVEL;
    }

}
