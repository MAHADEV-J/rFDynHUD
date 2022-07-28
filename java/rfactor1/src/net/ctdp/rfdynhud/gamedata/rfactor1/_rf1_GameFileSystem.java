/**
 * Copyright (C) 2009-2014 Cars and Tracks Development Project (CTDP).
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.ctdp.rfdynhud.gamedata.rfactor1;

import java.io.File;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.GameFileSystem;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.util.PluginINI;

import org.jagatoo.util.errorhandling.ParsingException;
import org.jagatoo.util.ini.AbstractIniParser;

class _rf1_GameFileSystem extends GameFileSystem
{
    private final File locationsFolder;
    private final String locationsPath;
    
    /**
     * Gets the game's &quot;Locations&quot; folder.
     * 
     * @return the game's &quot;Locations&quot; folder.
     */
    public final File getLocationsFolder()
    {
        return ( locationsFolder );
    }
    
    /**
     * Gets the game's &quot;Locations&quot; folder.
     * 
     * @return the game's &quot;Locations&quot; folder.
     */
    public final String getLocationsPath()
    {
        return ( locationsPath );
    }
    
    private static boolean isRoot( File folder )
    {
        return ( folder.getParent() == null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected File findGameFolderImpl( PluginINI pluginINI, File pluginFolder )
    {
        File f = pluginFolder.getParentFile();
        String exeFilename = pluginINI.getGeneralExeFilename();
        if ( exeFilename == null )
            exeFilename = "rFactor.exe";
        
        while ( !new File( f, exeFilename ).exists() && !isRoot( f ) )
            f = f.getParentFile();
        
        if ( new File( f, exeFilename ).exists() )
            return ( f.getAbsoluteFile() );
        
        if ( isRoot( pluginFolder ) )
            return ( pluginFolder.getAbsoluteFile() );
        
        return ( pluginFolder.getParentFile().getAbsoluteFile() );
    }
    
    /*
     * {@inheritDoc}
     */
    //@Override
    protected File findLocationsFolder( @SuppressWarnings( "unused" ) PluginINI pluginINI, @SuppressWarnings( "unused" ) File gameFolder )
    {
        return ( getPathFromGameConfigINI( "TracksDir", "GameData\\Locations\\" ) );
    }
    
    private static File getFallbackGameConfigINIPath( File gameFolder, String fallback )
    {
        File fallback0 = new File( gameFolder, fallback ).getAbsoluteFile();
        
        try
        {
            return ( fallback0.getCanonicalFile() );
        }
        catch ( IOException e )
        {
            return ( fallback0 );
        }
    }
    
    /**
     * Gets an absolute path from the game's config ini file.
     * 
     * @param setting the setting to query
     * @param fallback the fallback value, if the setting couldn't be read
     * 
     * @return the path as a File object.
     */
    public File getPathFromGameConfigINI( final String setting, String fallback )
    {
        File gameFolder = getGameFolder();
        File config = new File( gameFolder, "config.ini" );
        
        if ( !config.exists() )
        {
            return ( getFallbackGameConfigINIPath( gameFolder, fallback ) );
        }
        
        final String[] result = { null };
        
        try
        {
            new AbstractIniParser()
            {
                @Override
                protected boolean onSettingParsed( int lineNr, String group, String key, String value, String comment ) throws ParsingException
                {
                    if ( "COMPONENTS".equalsIgnoreCase( group ) && setting.equalsIgnoreCase( key ) )
                    {
                        result[0] = value;
                        
                        return ( false );
                    }
                    
                    return ( true );
                }
            }.parse( config );
        }
        catch ( ParsingException e1 )
        {
            return ( getFallbackGameConfigINIPath( gameFolder, fallback ) );
        }
        catch ( IOException e1 )
        {
            return ( getFallbackGameConfigINIPath( gameFolder, fallback ) );
        }
        
        if ( result[0] == null )
            return ( getFallbackGameConfigINIPath( gameFolder, fallback ) );
        
        File f = new File( result[0] );
        if ( f.isAbsolute() )
            return ( f );
        
        f = new File( gameFolder, result[0] );
        try
        {
            return ( f.getCanonicalFile() );
        }
        catch ( IOException e )
        {
            return ( f.getAbsoluteFile() );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public File locateSetupFile( LiveGameData gameData )
    {
        return ( new File( gameData.getProfileInfo().getProfileFolder(), "tempGarage.svm" ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected File findGameScreenshotsFolder( PluginINI pluginINI, File gameFolder )
    {
        return ( getPathFromGameConfigINI( "ScreenShotsDir", "UserData" + File.separator + "ScreenShots" ) );
    }
    
    public _rf1_GameFileSystem( PluginINI pluginINI )
    {
        super( _rf1_LiveGameDataObjectsFactory.GAME_ID, pluginINI );
        
        File gameFolder = findGameFolder( _rf1_LiveGameDataObjectsFactory.GAME_ID, pluginINI );
        
        this.locationsFolder = findLocationsFolder( pluginINI, gameFolder );
        this.locationsPath = locationsFolder.getAbsolutePath();
    }
}
