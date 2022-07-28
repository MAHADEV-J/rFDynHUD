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
package net.ctdp.rfdynhud.gamedata.rfactor2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.ctdp.rfdynhud.editor.EditorPresets;
import net.ctdp.rfdynhud.gamedata.ByteUtil;
import net.ctdp.rfdynhud.gamedata.GamePhase;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ProfileInfo.MeasurementUnits.Convert;
import net.ctdp.rfdynhud.gamedata.GameDataStreamSource;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.SessionType;
import net.ctdp.rfdynhud.gamedata.TelemVect3;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.gamedata.YellowFlagState;
import net.ctdp.rfdynhud.gamedata.__GDPrivilegedAccess;
import net.ctdp.rfdynhud.util.RFDHLog;

import org.jagatoo.util.streams.StreamUtils;

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
class _rf2_ScoringInfo extends ScoringInfo
{
    private static final int OFFSET_TRACK_NAME = 0;
    private static final int MAX_TRACK_NAME_LENGTH = 64;
    private static final int OFFSET_SESSION_TYPE = OFFSET_TRACK_NAME + MAX_TRACK_NAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_CURRENT_TIME = OFFSET_SESSION_TYPE + ByteUtil.SIZE_LONG;
    private static final int OFFSET_END_TIME = OFFSET_CURRENT_TIME + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_MAX_LAPS = OFFSET_END_TIME + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_LAP_DISTANCE = OFFSET_MAX_LAPS + ByteUtil.SIZE_LONG;
    
    private static final int OFFSET_RESULTS_STREAM = OFFSET_LAP_DISTANCE + ByteUtil.SIZE_DOUBLE;
    
    private static final int OFFSET_NUM_VEHICLES = OFFSET_RESULTS_STREAM + ByteUtil.SIZE_POINTER; // Is it just the pointer to be offsetted or the whole stream???
    
    private static final int OFFSET_GAME_PHASE = OFFSET_NUM_VEHICLES + ByteUtil.SIZE_LONG;
    private static final int OFFSET_YELLOW_FLAG_STATE = OFFSET_GAME_PHASE + ByteUtil.SIZE_CHAR;
    private static final int OFFSET_SECTOR_FLAGS = OFFSET_YELLOW_FLAG_STATE + ByteUtil.SIZE_CHAR;
    private static final int OFFSET_STARTING_LIGHT_FRAME = OFFSET_SECTOR_FLAGS + 3 * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_NUM_RED_LIGHTS = OFFSET_STARTING_LIGHT_FRAME + ByteUtil.SIZE_CHAR;
    
    private static final int OFFSET_IN_REALTIME = OFFSET_NUM_RED_LIGHTS + ByteUtil.SIZE_CHAR;
    
    private static final int OFFSET_PLAYER_NAME = OFFSET_IN_REALTIME + ByteUtil.SIZE_BOOL;
    private static final int MAX_PLAYER_NAME_LENGTH = 32;
    private static final int OFFSET_PLAYER_FILENAME = OFFSET_PLAYER_NAME + MAX_PLAYER_NAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int MAX_PLAYER_FILENAME_LENGTH = 64;
    
    private static final int OFFSET_CLOUD_DARKNESS = OFFSET_PLAYER_FILENAME + MAX_PLAYER_FILENAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_RAINING_SEVERITIY = OFFSET_CLOUD_DARKNESS + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_AMBIENT_TEMPERATURE = OFFSET_RAINING_SEVERITIY + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_TRACK_TEMPERATURE = OFFSET_AMBIENT_TEMPERATURE + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_WIND_SPEED = OFFSET_TRACK_TEMPERATURE + ByteUtil.SIZE_DOUBLE;
    private static final int OFFSET_ON_PATH_WETNESS = OFFSET_WIND_SPEED + ByteUtil.SIZE_VECTOR3D;
    private static final int OFFSET_OFF_PATH_WETNESS = OFFSET_ON_PATH_WETNESS + ByteUtil.SIZE_DOUBLE;
    
    private static final int OFFSET_EXPANSION = OFFSET_OFF_PATH_WETNESS + ByteUtil.SIZE_DOUBLE;
    
    private static final int OFFSET_VEHICLES = OFFSET_EXPANSION + ( 256 * ByteUtil.SIZE_CHAR );
    
    private static final int BUFFER_SIZE = OFFSET_VEHICLES + ByteUtil.SIZE_POINTER /*+ ( 256 * ByteUtil.SIZE_CHAR )*/; // + ( 1 * VehicleScoringInfo.BUFFER_SIZE ); // How many vehicles?
    
    private final byte[] buffer = new byte[ BUFFER_SIZE ];
    
    private static final java.net.URL DEFAULT_VALUES = _rf2_ScoringInfo.class.getClassLoader().getResource( _rf2_ScoringInfo.class.getPackage().getName().replace( '.', '/' ) + "/data/game_data/scoring_info" );
    
    private final _rf2_GraphicsInfo graphicsInfo;
    
    private static native void fetchData( final int numVehicles, final long sourceBufferAddress, final int sourceBufferSize, final byte[] targetBuffer, final long sourceBufferAddress2, final int sourceBufferSize2, final byte[] targetBuffer2 );
    
    @Override
    protected void updateDataImpl( int numVehicles, Object userObject, long timestamp )
    {
        if ( userObject instanceof _rf2_DataAddressKeeper2 )
        {
            _rf2_DataAddressKeeper2 ak = (_rf2_DataAddressKeeper2)userObject;
            
            _rf2_VehicleScoringInfo firstVSI = (_rf2_VehicleScoringInfo)getCachedVehicleScoringInfo( 0 );
            
            fetchData( numVehicles, ak.getBufferAddress(), ak.getBufferSize(), buffer, ak.getBufferAddress2(), ak.getBufferSize2(), ( firstVSI == null ) ? null : firstVSI.buffer );
        }
        else if ( userObject instanceof GameDataStreamSource )
        {
            InputStream in = ( (GameDataStreamSource)userObject ).getInputStreamForScoringInfo();
            
            if ( in != null )
            {
                try
                {
                    readFromStreamImpl( in );
                    
                    for ( int i = 0; i < numVehicles; i++ )
                    {
                        ( (_rf2_VehicleScoringInfo)getVehicleScoringInfo( i ) ).readFromStreamImpl( in );
                    }
                }
                catch ( IOException e )
                {
                    RFDHLog.exception( e );
                }
            }
        }
        else if ( userObject instanceof EditorPresets )
        {
            InputStream in = null;
            
            try
            {
                in = DEFAULT_VALUES.openStream();
                
                readFromStreamImpl( in );
                
                numVehicles = getNumVehiclesImpl();
                
                for ( int i = 0; i < numVehicles; i++ )
                {
                    ( (_rf2_VehicleScoringInfo)getCachedVehicleScoringInfo( i ) ).readFromStreamImpl( in );
                }
            }
            catch ( IOException e )
            {
                RFDHLog.exception( e );
            }
            finally
            {
                StreamUtils.closeStream( in );
            }
        }
    }
    
    private void readFromStreamImpl( InputStream in ) throws IOException
    {
        int offset = 0; //bufferOffset;
        int bytesToRead = BUFFER_SIZE;
        
        while ( bytesToRead > 0 )
        {
            int n = in.read( buffer, offset, bytesToRead );
            
            if ( n < 0 )
                throw new IOException();
            
            offset += n;
            bytesToRead -= n;
        }
    }
    
    @Override
    public void readFromStream( InputStream in, EditorPresets editorPresets ) throws IOException
    {
        final long now = System.nanoTime();
        
        readFromStreamImpl( in );
        
        int numVehicles = getNumVehiclesImpl();
        
        prepareDataUpdate( numVehicles, editorPresets, now );
        
        for ( int i = 0; i < numVehicles; i++ )
        {
            ( (_rf2_VehicleScoringInfo)getVehicleScoringInfo( i ) ).readFromStream( in );
        }
        
        if ( editorPresets != null )
        {
            applyEditorPresets( editorPresets );
            
            // Add postfixes to some vehicles' classes to get valid class-scoring in the editor.
            String classA = "F1 2006";
            String classB = "F1 2006B";
            __GDPrivilegedAccess.setVehicleClass( this, 0, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 1, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 2, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 3, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 4, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 5, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 6, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 7, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 8, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 9, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 10, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 11, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 12, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 13, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 14, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 15, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 16, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 17, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 18, classB );
            __GDPrivilegedAccess.setVehicleClass( this, 19, classA );
            __GDPrivilegedAccess.setVehicleClass( this, 20, classA );
            //__GDPrivilegedAccess.setVehicleClass( this, 21, classA );
        }
        
        onDataUpdated( numVehicles, editorPresets, now );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadDefaultValues( EditorPresets editorPresets )
    {
        InputStream in = null;
        
        try
        {
            in = DEFAULT_VALUES.openStream();
            
            readFromStream( in, editorPresets );
        }
        catch ( IOException e )
        {
            RFDHLog.exception( e );
        }
        finally
        {
            StreamUtils.closeStream( in );
        }
    }
    
    @Override
    public void writeToStream( OutputStream out ) throws IOException
    {
        out.write( buffer, 0, BUFFER_SIZE );
        
        int numVehicles = getNumVehiclesImpl();
        
        for ( int i = 0; i < numVehicles; i++ )
        {
            ( (_rf2_VehicleScoringInfo)getVehicleScoringInfo( i ) ).writeToStream( out );
        }
    }
    
    /*
     * ################################
     * ScoringInfoV01
     * ################################
     */
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTrackNameImpl()
    {
        // char mTrackName[64]
        
        return ( ByteUtil.readString( buffer, OFFSET_TRACK_NAME, MAX_TRACK_NAME_LENGTH ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final SessionType getSessionType()
    {
        // long mSession
        
        int st = (int)ByteUtil.readLong( buffer, OFFSET_SESSION_TYPE );
        
        switch ( st )
        {
            case 0:
                return ( SessionType.TEST_DAY );
            case 1:
                return ( SessionType.PRACTICE1 );
            case 2:
                return ( SessionType.PRACTICE2 );
            case 3:
                return ( SessionType.PRACTICE3 );
            case 4:
                return ( SessionType.PRACTICE4 );
            case 5:
                return ( SessionType.QUALIFYING1 );
            case 6:
                return ( SessionType.QUALIFYING2 );
            case 7:
                return ( SessionType.QUALIFYING3 );
            case 8:
                return ( SessionType.QUALIFYING4 );
            case 9:
                return ( SessionType.WARMUP );
            case 10:
                return ( SessionType.RACE1 );
            case 11:
                return ( SessionType.RACE2 );
            case 12:
                return ( SessionType.RACE3 );
            case 13:
                return ( SessionType.RACE4 );
        }
        
        // unreachable code
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final float getSessionTimeImpl()
    {
        // double mCurrentET
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_CURRENT_TIME ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getEndTime()
    {
        // double mEndET
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_END_TIME ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getMaxLaps()
    {
        // long mMaxLaps
        
        return ( (int)ByteUtil.readLong( buffer, OFFSET_MAX_LAPS ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final float getTrackLengthImpl()
    {
        // double mLapDist
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_LAP_DISTANCE ) );
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final int getNumVehiclesImpl()
    {
        // long mNumVehicles
        
        return ( (int)ByteUtil.readLong( buffer, OFFSET_NUM_VEHICLES ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final GamePhase getGamePhase()
    {
        // unsigned char mGamePhase
        
        short state = ByteUtil.readUnsignedByte( buffer, OFFSET_GAME_PHASE );
        
        switch ( state )
        {
            case 0:
                return ( GamePhase.BEFORE_SESSION_HAS_BEGUN );
            case 1:
                return ( GamePhase.RECONNAISSANCE_LAPS );
            case 2:
                return ( GamePhase.GRID_WALK_THROUGH );
            case 3:
                return ( GamePhase.FORMATION_LAP );
            case 4:
                return ( GamePhase.STARTING_LIGHT_COUNTDOWN_HAS_BEGUN );
            case 5:
                return ( GamePhase.GREEN_FLAG );
            case 6:
                return ( GamePhase.FULL_COURSE_YELLOW );
            case 7:
                return ( GamePhase.SESSION_STOPPED );
            case 8:
                return ( GamePhase.SESSION_OVER );
        }
        
        throw new Error( "Unknown game state read (" + state + ")." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final YellowFlagState getYellowFlagState()
    {
        // signed char mYellowFlagState
        
        short state = ByteUtil.readByte( buffer, OFFSET_YELLOW_FLAG_STATE );
        
        switch ( state )
        {
            case -1:
                throw new Error( "Invalid state detected." );
            case 0:
                return ( YellowFlagState.NONE );
            case 1:
                return ( YellowFlagState.PENDING );
            case 2:
                return ( YellowFlagState.PITS_CLOSED );
            case 3:
                return ( YellowFlagState.PIT_LEAD_LAP );
            case 4:
                return ( YellowFlagState.PITS_OPEN );
            case 5:
                return ( YellowFlagState.LAST_LAP );
            case 6:
                return ( YellowFlagState.RESUME );
            case 7:
                return ( YellowFlagState.RACE_HALT );
        }
        
        throw new Error( "Unknown game state read (" + state + ")." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean getSectorYellowFlag( int sector )
    {
        // signed char mSectorFlag[3]
        
        if ( ( sector < 1 ) || ( sector > 3 ) )
            throw new IllegalArgumentException( "Sector must be in range [1, 3]" );
        
        short flag = ByteUtil.readByte( buffer, OFFSET_SECTOR_FLAGS + ( sector % 3 ) );
        
        return ( flag != 0 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getStartLightFrame()
    {
        // unsigned char mStartLight
        
        return ( ByteUtil.readUnsignedByte( buffer, OFFSET_STARTING_LIGHT_FRAME ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNumStartingLights()
    {
        // unsigned char mNumRedLights
        
        return ( ByteUtil.readUnsignedByte( buffer, OFFSET_NUM_RED_LIGHTS ) );
    }
    /*
    public final boolean isInRealtimeMode()
    {
        // bool mInRealtime
        
        return ( ByteUtil.readBoolean( buffer, OFFSET_IN_REALTIME ) );
    }
    */
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getPlayerNameImpl()
    {
        // char mPlayerName[32]
        
        return ( ByteUtil.readString( buffer, OFFSET_PLAYER_NAME, MAX_PLAYER_NAME_LENGTH ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getPlayerFilenameImpl()
    {
        // char mPlrFileName[64]
        
        return ( ByteUtil.readString( buffer, OFFSET_PLAYER_FILENAME, MAX_PLAYER_FILENAME_LENGTH ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewedVehicleScoringInfo( VehicleScoringInfo vsi, int cameraType )
    {
        graphicsInfo.setViewedVehicleScoringInfo( (_rf2_VehicleScoringInfo)vsi, cameraType );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final _rf2_VehicleScoringInfo getViewedVehicleScoringInfoImpl()
    {
        _rf2_GraphicsInfo gi = (_rf2_GraphicsInfo)gameData.getGraphicsInfo();
        
        //if ( !gi.isUpdatedInRealtimeMode() )
        //    return ( getPlayersVehicleScoringInfo() );
        
        int viewedSlot = gi.getSlotID();
        
        final int n = getNumVehicles();
        for ( int i = 0; i < n; i++ )
        {
            _rf2_VehicleScoringInfo vsi = (_rf2_VehicleScoringInfo)getVehicleScoringInfo( i );
            
            if ( vsi.getSlotId() == viewedSlot )
                return ( vsi );
        }
        
        return ( (_rf2_VehicleScoringInfo)getPlayersVehicleScoringInfo() );
    }
    
    final float _getCloudDarkness()
    {
        // double mDarkCloud
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_CLOUD_DARKNESS ) );
    }
    
    final float _getRainingSeverity()
    {
        // double mRaining
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_RAINING_SEVERITIY ) );
    }
    
    final float _getAmbientTemperatureK()
    {
        // double mAmbientTemp
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_AMBIENT_TEMPERATURE ) - Convert.ZERO_KELVIN );
    }
    
    final float _getTrackTemperatureK()
    {
        // double mTrackTemp
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_TRACK_TEMPERATURE ) - Convert.ZERO_KELVIN );
    }
    
    final void _getWindSpeedMS( TelemVect3 speed )
    {
        // TelemVect3 mWind
        
        ByteUtil.readVectorD( buffer, OFFSET_WIND_SPEED, speed );
    }
    
    final float _getOnPathWetness()
    {
        // double mOnPathWetness
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_ON_PATH_WETNESS ) );
    }
    
    final float _getOffPathWetness()
    {
        // double mOffPathWetness
        
        return ( (float)ByteUtil.readDouble( buffer, OFFSET_OFF_PATH_WETNESS ) );
    }
    
    _rf2_ScoringInfo( LiveGameData gameData )
    {
        super( gameData );
        
        this.graphicsInfo = (_rf2_GraphicsInfo)gameData.getGraphicsInfo();
    }
}
