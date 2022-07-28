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
package net.ctdp.rfdynhud.gamedata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import net.ctdp.rfdynhud.input.InputAction;
import net.ctdp.rfdynhud.input.InputActionConsumer;
import net.ctdp.rfdynhud.input.__InpPrivilegedAccess;
import net.ctdp.rfdynhud.util.RFDHLog;

import org.jagatoo.util.xml.SimpleXMLHandler;
import org.jagatoo.util.xml.SimpleXMLParser;
import org.jagatoo.util.xml.SimpleXMLWriter;
import org.jagatoo.util.xml.XMLPath;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class DataCache implements LiveGameData.GameDataUpdateListener, InputActionConsumer
{
    private static final int CURRENT_VERSION_MAJOR = 1;
    private static final int CURRENT_VERSION_MINOR = 2;
    private static final int CURRENT_VERSION_REVISION = 0;
    private static final String CURRENT_VERSION_STRING = CURRENT_VERSION_MAJOR + "." + CURRENT_VERSION_MINOR + "." + CURRENT_VERSION_REVISION;
    
    @Override
    public void onBoundInputStateChanged( InputAction action, boolean state, int modifierMask, long when, LiveGameData gameData, boolean isEditorMode )
    {
        if ( action == INPUT_ACTION_RESET_LAPTIMES_CACHE )
        {
            liveReset( gameData );
        }
    }
    
    static final DataCache INSTANCE = new DataCache();
    static final InputAction INPUT_ACTION_RESET_LAPTIMES_CACHE = __InpPrivilegedAccess.createInputAction( "ResetLaptimesCache", true, false, INSTANCE, DataCache.class.getClassLoader().getResource( DataCache.class.getPackage().getName().replace( '.', '/' ) + "/doc/ResetLaptimesCache.html" ) );
    
    private static class VersionException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        
        public VersionException( String message )
        {
            super( message );
        }
        
        public static final void checkVersion( String version, int maxMajor, int maxMinor, int maxRevision ) throws VersionException
        {
            int[] ver = null;
            try
            {
                String[] ss = version.split( "\\." );
                ver = new int[ ss.length ];
                for ( int i = 0; i < ss.length; i++ )
                    ver[i] = Integer.parseInt( ss[i] );
            }
            catch ( Throwable t )
            {
                throw new VersionException( "Error parsing version from configuration file." );
            }
            
            int[] testVer = { maxMajor, maxMinor, maxRevision };
            
            boolean isNewer = false;
            
            for ( int i = 0; i < ver.length && !isNewer; i++ )
            {
                int tv = ( testVer.length > i ) ? testVer[i] : 0;
                
                if ( ver[i] > tv )
                    isNewer = true;
            }
            
            if ( isNewer )
                throw new VersionException( "The cache file has a newer format than this version of rfDynHUD is able to handle." );
        }
    }
    
    private final Map<String, Float> fuelUsages = new HashMap<String, Float>();
    private final Map<String, Laptime> fastestNormalLaptimes = new HashMap<String, Laptime>();
    private final Map<String, Laptime> fastestHotLaptimes = new HashMap<String, Laptime>();
    
    void liveReset( LiveGameData gameData )
    {
        final SessionType sessionType = gameData.getScoringInfo().getSessionType();
        
        Laptime.LapType lapType;
        if ( sessionType.isRace() )
            lapType = Laptime.LapType.RACE;
        else if ( sessionType.isQualifying() )
            lapType = Laptime.LapType.QUALIFY;
        else if ( Laptime.isHotlap( gameData ) )
            lapType = Laptime.LapType.HOTLAP;
        else
            lapType = Laptime.LapType.NORMAL;
        
        if ( lapType == Laptime.LapType.HOTLAP )
        {
            fastestHotLaptimes.clear();
            gameData.getScoringInfo().getPlayersVehicleScoringInfo().cachedFastestHotLaptime = null;
        }
        else
        {
            fastestNormalLaptimes.clear();
            gameData.getScoringInfo().getPlayersVehicleScoringInfo().cachedFastestNormalLaptime = null;
        }
    }
    
    static final boolean checkSessionType( ScoringInfo scoringInfo )
    {
        SessionType sessionType = scoringInfo.getSessionType();
        
        if ( sessionType == SessionType.TEST_DAY )
            return ( true );
        
        if ( sessionType == SessionType.PRACTICE1 )
            return ( scoringInfo.getNumVehicles() == 1 );
        
        return ( false );
    }
    
    void addLaptime( ScoringInfo scoringInfo, String teamName, Laptime laptime )
    {
        if ( !checkSessionType( scoringInfo ) )
            return;
        
        if ( laptime.getLapTime() <= 0f )
            return;
        
        Laptime cached;
        
        switch ( laptime.getType() )
        {
            case NORMAL:
                cached = fastestNormalLaptimes.get( teamName );
                if ( ( cached == null ) || ( laptime.getLapTime() < cached.getLapTime() ) )
                    fastestNormalLaptimes.put( teamName, laptime );
                break;
            case HOTLAP:
                cached = fastestHotLaptimes.get( teamName );
                if ( ( cached == null ) || ( laptime.getLapTime() < cached.getLapTime() ) )
                    fastestHotLaptimes.put( teamName, laptime );
                break;
            // We're not interested in other times.
        }
    }
    
    final Float getFuelUsage( String teamName )
    {
        return ( fuelUsages.get( teamName ) );
    }
    
    private static File getCacheFile( GameFileSystem fileSystem, String modName, String trackName, boolean createFolder )
    {
        File cacheFolder = fileSystem.getCacheFolder();
        
        if ( cacheFolder == null )
            return ( null );
        
        cacheFolder = new File( new File( cacheFolder, "data" ), modName );
        if ( createFolder )
        {
            try
            {
                cacheFolder.mkdirs();
            }
            catch ( Throwable t )
            {
                RFDHLog.exception( "WARNING: Failed to create cache folder. Data-Cache deactivated." );
            }
        }
        else if ( !cacheFolder.exists() )
        {
            return ( null );
        }
        
        return ( new File( cacheFolder, trackName + ".xml" ) );
    }
    
    private static File getCacheFile( LiveGameData gameData, boolean createFolder )
    {
        return ( getCacheFile( gameData.getFileSystem(), gameData.getModInfo().getName(), gameData.getScoringInfo().getTrackName(), createFolder ) );
    }
    
    private void loadFromCache( File cacheFile )
    {
        SimpleXMLHandler handler = new SimpleXMLHandler()
        {
            private String currentTeam = null;
            
            @Override
            protected void onElementStarted( XMLPath path, String name, Object object, Attributes attributes ) throws SAXException
            {
                if ( ( path.getLevel() == 0 ) && name.equals( "CachedData" ) )
                {
                    VersionException.checkVersion( attributes.getValue( "version" ), CURRENT_VERSION_MAJOR, CURRENT_VERSION_MINOR, CURRENT_VERSION_REVISION );
                }
                else if ( path.isAt( false, "CachedData" ) && name.equals( "VehicleData" ) )
                {
                    currentTeam = attributes.getValue( "vehicle" );
                }
                else if ( path.isAt( false, "CachedData", "VehicleData" ) )
                {
                    if ( name.equals( "FuelUsage" ) )
                    {
                        try
                        {
                            float avgFuelUsage = Float.parseFloat( attributes.getValue( "average" ) );
                            fuelUsages.put( currentTeam, avgFuelUsage );
                        }
                        catch ( NumberFormatException e )
                        {
                            RFDHLog.exception( "WARNING: DataCache: Unable to parse value \"" + attributes.getValue( "average" ) + "\" to a float for fuel usage." );
                        }
                    }
                    else if ( name.equals( "FastestLap" ) )
                    {
                        try
                        {
                            String type = attributes.getValue( "type" );
                            float sector1 = Float.parseFloat( attributes.getValue( "sector1" ) );
                            float sector2 = Float.parseFloat( attributes.getValue( "sector2" ) );
                            float sector3 = Float.parseFloat( attributes.getValue( "sector3" ) );
                            float lap = Float.parseFloat( attributes.getValue( "lap" ) );
                            
                            Laptime laptime = new Laptime( 0, 0, sector1, sector2, sector3, false, false, true );
                            laptime.laptime = lap;
                            
                            try
                            {
                                laptime.setType( Laptime.LapType.valueOf( type ) );
                            }
                            catch ( Throwable t )
                            {
                                laptime.setType( Laptime.LapType.UNKNOWN );
                            }
                            
                            switch ( laptime.getType() )
                            {
                                case NORMAL:
                                    fastestNormalLaptimes.put( currentTeam, laptime );
                                    break;
                                case HOTLAP:
                                    fastestHotLaptimes.put( currentTeam, laptime );
                                    break;
                                // We're not interested in other times.
                            }
                        }
                        catch ( NumberFormatException e )
                        {
                            RFDHLog.exception( "WARNING: DataCache: Unable to parse laptime." );
                        }
                    }
                }
            }
            
            @Override
            protected void onElementData( XMLPath path, Attributes attributes, char[] data, int start, int length ) throws SAXException
            {
            }
            
            @Override
            protected void onElementEnded( XMLPath path, String name, Object object ) throws SAXException
            {
                if ( ( path.getLevel() == 1 ) && name.equals( "VehicleData" ) )
                {
                    currentTeam = null;
                }
            }
            
            @Override
            protected void onParsingException( XMLPath path, ExceptionSeverity severity, SAXParseException ex ) throws SAXException
            {
                RFDHLog.exception( "XML parsing exception at " + path.toString() );
                RFDHLog.exception( ex );
            }
        };
        
        try
        {
            SimpleXMLParser.parseXML( cacheFile, handler );
        }
        catch ( ParserConfigurationException e )
        {
            RFDHLog.exception( e );
        }
        catch ( SAXException e )
        {
            RFDHLog.exception( e );
        }
        catch ( IOException e )
        {
            RFDHLog.exception( e );
        }
        catch ( VersionException e )
        {
            RFDHLog.exception( "ERROR: " + e.getMessage() );
        }
    }
    
    static Float loadFuelUsageFromCache( GameFileSystem fileSystem, final String modName, final String trackName, final String teamName )
    {
        File cacheFile = getCacheFile( fileSystem, modName, trackName, false );
        
        if ( cacheFile == null )
            return ( null );
        
        final Float[] result = { null };
        
        SimpleXMLHandler handler = new SimpleXMLHandler()
        {
            private String currentTeam = null;
            
            @Override
            protected void onElementStarted( XMLPath path, String name, Object object, Attributes attributes ) throws SAXException
            {
                if ( ( path.getLevel() == 0 ) && name.equals( "CachedData" ) )
                {
                    VersionException.checkVersion( attributes.getValue( "version" ), CURRENT_VERSION_MAJOR, CURRENT_VERSION_MINOR, CURRENT_VERSION_REVISION );
                }
                else if ( path.isAt( false, "CachedData" ) && name.equals( "VehicleData" ) )
                {
                    currentTeam = attributes.getValue( "vehicle" );
                }
                else if ( path.isAt( false, "CachedData", "VehicleData" ) && name.equals( "FuelUsage" ) )
                {
                    try
                    {
                        float avgFuelUsage = Float.parseFloat( attributes.getValue( "average" ) );
                        if ( teamName.equals( currentTeam ) )
                            result[0] = avgFuelUsage;
                    }
                    catch ( NumberFormatException e )
                    {
                        RFDHLog.exception( "WARNING: DataCache: Unable to parse value \"" + attributes.getValue( "average" ) + "\" to a float for fuel usage." );
                    }
                }
            }
            
            @Override
            protected void onElementData( XMLPath path, Attributes attributes, char[] data, int start, int length ) throws SAXException
            {
            }
            
            @Override
            protected void onElementEnded( XMLPath path, String name, Object object ) throws SAXException
            {
                if ( ( path.getLevel() == 1 ) && name.equals( "VehicleData" ) )
                {
                    currentTeam = null;
                }
            }
            
            @Override
            protected void onParsingException( XMLPath path, ExceptionSeverity severity, SAXParseException ex ) throws SAXException
            {
                RFDHLog.exception( "XML parsing exception at " + path.toString() );
                RFDHLog.exception( ex );
            }
        };
        
        try
        {
            SimpleXMLParser.parseXML( cacheFile, handler );
        }
        catch ( ParserConfigurationException e )
        {
            RFDHLog.exception( e );
        }
        catch ( SAXException e )
        {
            RFDHLog.exception( e );
        }
        catch ( IOException e )
        {
            RFDHLog.exception( e );
        }
        catch ( VersionException e )
        {
            RFDHLog.exception( "Error: " + e.getMessage() );
        }
        
        return ( result[0] );
    }
    
    @Override
    public void onSessionStarted( LiveGameData gameData, boolean isEditorMode )
    {
        fuelUsages.clear();
        fastestNormalLaptimes.clear();
        fastestHotLaptimes.clear();
        
        VehicleScoringInfo player = gameData.getScoringInfo().getPlayersVehicleScoringInfo();
        
        player.cachedFastestNormalLaptime = null;
        player.cachedFastestHotLaptime = null;
        
        File cacheFile = getCacheFile( gameData, false );
        
        if ( ( cacheFile == null ) || !cacheFile.exists() )
            return;
        
        loadFromCache( cacheFile );
        
        if ( !checkSessionType( gameData.getScoringInfo() ) )
            return;
        
        player.cachedFastestNormalLaptime = fastestNormalLaptimes.get( gameData.getProfileInfo().getTeamName() );
        player.cachedFastestHotLaptime = fastestHotLaptimes.get( gameData.getProfileInfo().getTeamName() );
    }
    
    @Override
    public void onCockpitEntered( LiveGameData gameData, boolean isEditorMode ) {}
    
    @Override
    public void onGamePauseStateChanged( LiveGameData gameData, boolean isEditorMode, boolean isPaused ) {}
    
    private void storeToCache( LiveGameData gameData )
    {
        File cacheFile = getCacheFile( gameData, true );
        
        if ( cacheFile == null )
            return;
        
        ArrayList<String> vehicleNames = new ArrayList<String>( fuelUsages.keySet() );
        Collections.sort( vehicleNames );
        
        SimpleXMLWriter writer = null;
        
        try
        {
            writer = new SimpleXMLWriter( cacheFile );
            
            writer.writeElementAndPush( "CachedData", "version", CURRENT_VERSION_STRING );
            
            for ( String vehicleName : vehicleNames )
            {
                writer.writeElementAndPush( "VehicleData", "vehicle", vehicleName );
                
                Float fuelUsage = fuelUsages.get( vehicleName );
                if ( fuelUsage != null )
                    writer.writeElement( "FuelUsage", "average", fuelUsage );
                
                Laptime laptime = fastestNormalLaptimes.get( vehicleName );
                if ( laptime != null )
                    writer.writeElement( "FastestLap", "type", laptime.getType().name(), "sector1", laptime.getSector1(), "sector2", laptime.getSector2(), "sector3", laptime.getSector3(), "lap", laptime.getLapTime() );
                
                laptime = fastestHotLaptimes.get( vehicleName );
                if ( laptime != null )
                    writer.writeElement( "FastestLap", "type", laptime.getType().name(), "sector1", laptime.getSector1(), "sector2", laptime.getSector2(), "sector3", laptime.getSector3(), "lap", laptime.getLapTime() );
                
                writer.popElement();
            }
            
            writer.popElement();
        }
        catch ( IOException e )
        {
            RFDHLog.exception( e );
        }
        catch ( SAXException e )
        {
            RFDHLog.exception( e );
        }
        finally
        {
            try { writer.close(); } catch ( Throwable t ) {}
        }
    }
    
    @Override
    public void onCockpitExited( LiveGameData gameData, boolean isEditorMode )
    {
        //VehicleScoringInfo player = gameData.getScoringInfo().getPlayersVehicleScoringInfo();
        //String teamName = player.getVehicleName();
        String teamName = gameData.getProfileInfo().getTeamName();
        
        float avgFuelUsage = FuelUsageRecorder.MASTER_FUEL_USAGE_RECORDER.getAverage();
        if ( avgFuelUsage > 0f )
            fuelUsages.put( teamName, avgFuelUsage );
        
        //fastestLaptimes.put( teamName, player.getFastestLaptime() );
        
        storeToCache( gameData );
    }
    
    private DataCache()
    {
    }
}
