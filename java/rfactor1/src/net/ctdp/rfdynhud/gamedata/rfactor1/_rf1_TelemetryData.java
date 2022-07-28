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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.ctdp.rfdynhud.editor.EditorPresets;
import net.ctdp.rfdynhud.gamedata.ByteUtil;
import net.ctdp.rfdynhud.gamedata.DeviceLegalStatus;
import net.ctdp.rfdynhud.gamedata.IgnitionStatus;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ProfileInfo.MeasurementUnits.Convert;
import net.ctdp.rfdynhud.gamedata.GameDataStreamSource;
import net.ctdp.rfdynhud.gamedata.SurfaceType;
import net.ctdp.rfdynhud.gamedata.TelemVect3;
import net.ctdp.rfdynhud.gamedata.TelemetryData;
import net.ctdp.rfdynhud.gamedata.VehicleSetup;
import net.ctdp.rfdynhud.gamedata.Wheel;
import net.ctdp.rfdynhud.gamedata.WheelPart;
import net.ctdp.rfdynhud.util.RFDHLog;

import org.jagatoo.util.streams.StreamUtils;

/**
 * Our world coordinate system is left-handed, with +y pointing up.
 * The local vehicle coordinate system is as follows:
 *   +x points out the left side of the car (from the driver's perspective)
 *   +y points out the roof
 *   +z points out the back of the car
 * Rotations are as follows:
 *   +x pitches up
 *   +y yaws to the right
 *   +z rolls to the right
 * 
 * @author Marvin Froehlich (CTDP)
 */
class _rf1_TelemetryData extends TelemetryData
{
    private static final int OFFSET_DELTA_TIME = 0;
    private static final int OFFSET_LAP_NUMBER = OFFSET_DELTA_TIME + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_LAP_START_TIME = OFFSET_LAP_NUMBER + ByteUtil.SIZE_LONG;
    private static final int OFFSET_VEHICLE_NAME = OFFSET_LAP_START_TIME + ByteUtil.SIZE_FLOAT;
    private static final int MAX_VEHICLE_NAME_LENGTH = 64;
    private static final int OFFSET_TRACK_NAME = OFFSET_VEHICLE_NAME + MAX_VEHICLE_NAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int MAX_TRACK_NAME_LENGTH = 64;
    
    private static final int OFFSET_POSITION = OFFSET_TRACK_NAME + MAX_TRACK_NAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_LOCAL_VELOCITY = OFFSET_POSITION + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_LOCAL_ACCELERATION = OFFSET_LOCAL_VELOCITY + ByteUtil.SIZE_VECTOR3F;
    
    private static final int OFFSET_ORIENTATION_X = OFFSET_LOCAL_ACCELERATION + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_ORIENTATION_Y = OFFSET_ORIENTATION_X + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_ORIENTATION_Z = OFFSET_ORIENTATION_Y + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_LOCAL_ROTATION = OFFSET_ORIENTATION_Z + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_LOCAL_ROTATION_ACCELERATION = OFFSET_LOCAL_ROTATION + ByteUtil.SIZE_VECTOR3F;
    
    private static final int OFFSET_GEAR = OFFSET_LOCAL_ROTATION_ACCELERATION + ByteUtil.SIZE_VECTOR3F;
    private static final int OFFSET_ENGINE_RPM = OFFSET_GEAR + ByteUtil.SIZE_LONG;
    private static final int OFFSET_ENGINE_WATER_TEMP = OFFSET_ENGINE_RPM + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_ENGINE_OIL_TEMP = OFFSET_ENGINE_WATER_TEMP + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_CLUTCH_RPM = OFFSET_ENGINE_OIL_TEMP + ByteUtil.SIZE_FLOAT;
    
    private static final int OFFSET_UNFILTERED_THROTTLE = OFFSET_CLUTCH_RPM + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_UNFILTERED_BRAKE = OFFSET_UNFILTERED_THROTTLE + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_UNFILTERED_STEERING = OFFSET_UNFILTERED_BRAKE + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_UNFILTERED_CLUTCH = OFFSET_UNFILTERED_STEERING + ByteUtil.SIZE_FLOAT;
    
    private static final int OFFSET_STEERING_ARM_FORCE = OFFSET_UNFILTERED_CLUTCH + ByteUtil.SIZE_FLOAT;
    
    private static final int OFFSET_FUEL = OFFSET_STEERING_ARM_FORCE + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_ENGINE_MAX_RPM = OFFSET_FUEL + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_SCHEDULED_STOPS = OFFSET_ENGINE_MAX_RPM + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_OVERHEATING = OFFSET_SCHEDULED_STOPS + ByteUtil.SIZE_CHAR;
    private static final int OFFSET_DETACHED = OFFSET_OVERHEATING + ByteUtil.SIZE_BOOL;
    private static final int OFFSET_DENT_SEVERITY = OFFSET_DETACHED + ByteUtil.SIZE_BOOL;
    private static final int OFFSET_LAST_IMPACT_TIME = OFFSET_DENT_SEVERITY + 8 * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_LAST_IMPACT_MAGNITUDE = OFFSET_LAST_IMPACT_TIME + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_LAST_IMPACT_POSITION = OFFSET_LAST_IMPACT_MAGNITUDE + ByteUtil.SIZE_FLOAT;
    
    private static final int OFFSET_EXPANSION = OFFSET_LAST_IMPACT_POSITION + ByteUtil.SIZE_VECTOR3F;
    
    private static final int OFFSET_WHEEL_DATA = OFFSET_EXPANSION + 64 * ByteUtil.SIZE_CHAR;
    
    private static final int OFFSET_WHEEL_ROTATION = 1;
    private static final int OFFSET_WHEEL_SUSPENSION_DEFLECTION = OFFSET_WHEEL_ROTATION + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_RIDE_HEIGHT = OFFSET_WHEEL_SUSPENSION_DEFLECTION + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_TIRE_LOAD = OFFSET_RIDE_HEIGHT + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_LATERAL_FORCE = OFFSET_TIRE_LOAD + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_GRIP_FRACTION = OFFSET_LATERAL_FORCE + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_BRAKE_TEMP = OFFSET_GRIP_FRACTION + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_TIRE_PRESSURE = OFFSET_BRAKE_TEMP + ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_TIRE_TEMPERATURES = OFFSET_TIRE_PRESSURE + ByteUtil.SIZE_FLOAT;
    
    private static final int OFFSET_TIRE_WEAR = OFFSET_TIRE_TEMPERATURES + 3 * ByteUtil.SIZE_FLOAT;
    private static final int OFFSET_TERRAIN_NAME = OFFSET_TIRE_WEAR + ByteUtil.SIZE_FLOAT;
    private static final int MAX_TERRAIN_NAME_LENGTH = 16;
    private static final int OFFSET_SURFACE_TYPE = OFFSET_TERRAIN_NAME + MAX_TERRAIN_NAME_LENGTH * ByteUtil.SIZE_CHAR;
    private static final int OFFSET_IS_WHEEL_FLAT = OFFSET_SURFACE_TYPE + ByteUtil.SIZE_CHAR;
    private static final int OFFSET_IS_WHEEL_DETACHED = OFFSET_IS_WHEEL_FLAT + ByteUtil.SIZE_BOOL;
    
    private static final int OFFSET_WHEEL_DATA_EXPENSION = OFFSET_IS_WHEEL_DETACHED + ByteUtil.SIZE_BOOL;
    
    private static final int WHEEL_DATA_SIZE = OFFSET_WHEEL_DATA_EXPENSION + 32 * ByteUtil.SIZE_CHAR;
    
    private static final int BUFFER_SIZE = OFFSET_WHEEL_DATA + ( 4 * WHEEL_DATA_SIZE );
    
    private final byte[] buffer = new byte[ BUFFER_SIZE ];
    
    private static final java.net.URL DEFAULT_VALUES = _rf1_TelemetryData.class.getClassLoader().getResource( _rf1_TelemetryData.class.getPackage().getName().replace( '.', '/' ) + "/data/game_data/telemetry_data" );
    
    float engineLifetime = 0.0f;
    float brakeDiscThicknessFL = 0.0f;
    float brakeDiscThicknessFR = 0.0f;
    float brakeDiscThicknessRL = 0.0f;
    float brakeDiscThicknessRR = 0.0f;
    
    private static native void fetchData( final long sourceBufferAddress, final int sourceBufferSize, final byte[] targetBuffer );
    
    @Override
    protected void updateDataImpl( Object userObject, long timestamp )
    {
        if ( userObject instanceof _rf1_DataAddressKeeper )
        {
            _rf1_DataAddressKeeper ak = (_rf1_DataAddressKeeper)userObject;
            
            fetchData( ak.getBufferAddress(), ak.getBufferSize(), buffer );
        }
        else if ( userObject instanceof GameDataStreamSource )
        {
            InputStream in = ( (GameDataStreamSource)userObject ).getInputStreamForTelemetryData();
            
            if ( in != null )
            {
                try
                {
                    readFromStreamImpl( in );
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
        int offset = 0;
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
        
        prepareDataUpdate( null, now );
        
        readFromStreamImpl( in );
        
        onDataUpdated( editorPresets, now );
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
    }
    
    @Override
    protected void applyEditorPresets( EditorPresets editorPresets )
    {
        super.applyEditorPresets( editorPresets );
        
        if ( editorPresets == null )
            return;
        
        this.engineLifetime = editorPresets.getEngineLifetime();
        
        this.brakeDiscThicknessFL = editorPresets.getBrakeDiscThicknessFL();
        this.brakeDiscThicknessFR = editorPresets.getBrakeDiscThicknessFR();
        this.brakeDiscThicknessRL = editorPresets.getBrakeDiscThicknessRL();
        this.brakeDiscThicknessRR = editorPresets.getBrakeDiscThicknessRR();
    }
    
    @Override
    protected void onCockpitEntered( long timestamp )
    {
        super.onCockpitEntered( timestamp );
        
        this.engineLifetime = gameData.getPhysics().getEngine().getSafeLifetimeTotal( 1.0 );
        
        VehicleSetup setup = gameData.getSetup();
        this.brakeDiscThicknessFL = setup.getWheelAndTire( Wheel.FRONT_LEFT ).getBrakeDiscThickness();
        this.brakeDiscThicknessFR = setup.getWheelAndTire( Wheel.FRONT_RIGHT ).getBrakeDiscThickness();
        this.brakeDiscThicknessRL = setup.getWheelAndTire( Wheel.REAR_LEFT ).getBrakeDiscThickness();
        this.brakeDiscThicknessRR = setup.getWheelAndTire( Wheel.REAR_RIGHT ).getBrakeDiscThickness();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getEngineLifetime()
    {
        return ( engineLifetime );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getBrakeDiscThicknessM( Wheel wheel )
    {
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( brakeDiscThicknessFL );
            case FRONT_RIGHT:
                return ( brakeDiscThicknessFR );
            case REAR_LEFT:
                return ( brakeDiscThicknessRL );
            case REAR_RIGHT:
                return ( brakeDiscThicknessRR );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getDeltaTime()
    {
        // float mDeltaTime
        
        return ( ByteUtil.readFloat( buffer, OFFSET_DELTA_TIME ) );
    }
    
    //@Override
    public final int getCurrentLapNumber()
    {
        // long mLapNumber
        
        return ( (int)ByteUtil.readLong( buffer, OFFSET_LAP_NUMBER ) );
    }
    
    //@Override
    public final float getLapStartET()
    {
        // float mLapStartET
        
        return ( ByteUtil.readFloat( buffer, OFFSET_LAP_START_TIME ) );
    }
    
    //@Override
    public final String getVehicleName()
    {
        // char mVehicleName[64]
        
        return ( ByteUtil.readString( buffer, OFFSET_VEHICLE_NAME, MAX_VEHICLE_NAME_LENGTH ) );
    }
    
    //@Override
    public final String getTrackName()
    {
        // char mTrackName[64]
        
        return ( ByteUtil.readString( buffer, OFFSET_TRACK_NAME, MAX_TRACK_NAME_LENGTH ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getPosition( TelemVect3 position )
    {
        // TelemVect3 mPos
        
        ByteUtil.readVectorF( buffer, OFFSET_POSITION, position );
        
        return ( position );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getPositionX()
    {
        // TelemVect3 mPos
        
        return ( ByteUtil.readFloat( buffer, OFFSET_POSITION + 0 * ByteUtil.SIZE_FLOAT ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getPositionY()
    {
        // TelemVect3 mPos
        
        return ( ByteUtil.readFloat( buffer, OFFSET_POSITION + 1 * ByteUtil.SIZE_FLOAT ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getPositionZ()
    {
        // TelemVect3 mPos
        
        return ( ByteUtil.readFloat( buffer, OFFSET_POSITION + 2 * ByteUtil.SIZE_FLOAT ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getLocalVelocity( TelemVect3 localVel )
    {
        // TelemVect3 mLocalVel
        
        ByteUtil.readVectorF( buffer, OFFSET_LOCAL_VELOCITY, localVel );
        
        return ( localVel );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getScalarVelocityMS()
    {
        float vecX = ByteUtil.readFloat( buffer, OFFSET_LOCAL_VELOCITY + 0 * ByteUtil.SIZE_FLOAT );
        float vecY = ByteUtil.readFloat( buffer, OFFSET_LOCAL_VELOCITY + 1 * ByteUtil.SIZE_FLOAT );
        float vecZ = ByteUtil.readFloat( buffer, OFFSET_LOCAL_VELOCITY + 2 * ByteUtil.SIZE_FLOAT );
        
        return ( (float)Math.sqrt( vecX * vecX + vecY * vecY + vecZ * vecZ ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getLocalAcceleration( TelemVect3 localAccel )
    {
        // TelemVect3 mLocalAccel
        
        ByteUtil.readVectorF( buffer, OFFSET_LOCAL_ACCELERATION, localAccel );
        
        return ( localAccel );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getLongitudinalAcceleration()
    {
        // TelemVect3 mLocalAccel
        
        return ( -ByteUtil.readFloat( buffer, OFFSET_LOCAL_ACCELERATION + 2 * ByteUtil.SIZE_FLOAT ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getLateralAcceleration()
    {
        // TelemVect3 mLocalAccel
        
        return ( ByteUtil.readFloat( buffer, OFFSET_LOCAL_ACCELERATION + 0 * ByteUtil.SIZE_FLOAT ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getOrientationX( TelemVect3 oriX )
    {
        // TelemVect3 mOriX
        
        ByteUtil.readVectorF( buffer, OFFSET_ORIENTATION_X, oriX );
        
        return ( oriX );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getOrientationY( TelemVect3 oriY )
    {
        // TelemVect3 mOriY
        
        ByteUtil.readVectorF( buffer, OFFSET_ORIENTATION_Y, oriY );
        
        return ( oriY );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getOrientationZ( TelemVect3 oriZ )
    {
        // TelemVect3 mOriZ
        
        ByteUtil.readVectorF( buffer, OFFSET_ORIENTATION_Z, oriZ );
        
        return ( oriZ );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getLocalRotation( TelemVect3 localRot )
    {
        // TelemVect3 mLocalRot
        
        ByteUtil.readVectorF( buffer, OFFSET_LOCAL_ROTATION, localRot );
        
        return ( localRot );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getLocalRotationalAcceleration( TelemVect3 localRotAccel )
    {
        // TelemVect3 mLocalRotAccel
        
        ByteUtil.readVectorF( buffer, OFFSET_LOCAL_ROTATION_ACCELERATION, localRotAccel );
        
        return ( localRotAccel );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final short getCurrentGear()
    {
        // long mGear
        
        return ( (short)ByteUtil.readLong( buffer, OFFSET_GEAR ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final float getEngineRPMImpl()
    {
        // float mEngineRPM
        
        return ( ByteUtil.readFloat( buffer, OFFSET_ENGINE_RPM ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getEngineWaterTemperatureC()
    {
        // float mEngineWaterTemp
        
        return ( ByteUtil.readFloat( buffer, OFFSET_ENGINE_WATER_TEMP ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getEngineOilTemperatureC()
    {
        // float mEngineOilTemp
        
        return ( ByteUtil.readFloat( buffer, OFFSET_ENGINE_OIL_TEMP ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getClutchRPM()
    {
        // float mClutchRPM
        
        return ( ByteUtil.readFloat( buffer, OFFSET_CLUTCH_RPM ) );
    }
    
    // Driver input
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getUnfilteredThrottle()
    {
        // float mUnfilteredThrottle
        
        return ( ByteUtil.readFloat( buffer, OFFSET_UNFILTERED_THROTTLE ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getUnfilteredBrake()
    {
        // float mUnfilteredBrake
        
        return ( ByteUtil.readFloat( buffer, OFFSET_UNFILTERED_BRAKE ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getUnfilteredClutch()
    {
        // float mUnfilteredClutch
        
        return ( ByteUtil.readFloat( buffer, OFFSET_UNFILTERED_CLUTCH ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getUnfilteredSteering()
    {
        // float mUnfilteredSteering
        
        return ( ByteUtil.readFloat( buffer, OFFSET_UNFILTERED_STEERING ) );
    }
    
    // Misc
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getSteeringArmForce()
    {
        // float mSteeringArmForce
        
        return ( ByteUtil.readFloat( buffer, OFFSET_STEERING_ARM_FORCE ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final float getFuelImpl()
    {
        // float mFuel
        
        return ( ByteUtil.readFloat( buffer, OFFSET_FUEL ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final float getEngineMaxRPMImpl()
    {
        // float mEngineMaxRPM
        
        return ( ByteUtil.readFloat( buffer, OFFSET_ENGINE_MAX_RPM ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final short getNumberOfScheduledPitstops()
    {
        // unsigned char mScheduledStops
        
        return ( ByteUtil.readUnsignedByte( buffer, OFFSET_SCHEDULED_STOPS ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isOverheating()
    {
        // bool mOverheating
        
        return ( ByteUtil.readBoolean( buffer, OFFSET_OVERHEATING ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAnythingDetached()
    {
        // bool mDetached
        
        return ( ByteUtil.readBoolean( buffer, OFFSET_DETACHED ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final short[] getDentSevirity()
    {
        // unsigned char mDentSeverity[8]
        
        short[] result = new short[ 8 ];
        
        for ( int i = 0; i < result.length; i++ )
        {
            result[i] = ByteUtil.readUnsignedByte( buffer, OFFSET_DENT_SEVERITY + i * ByteUtil.SIZE_CHAR );
        }
        
        return ( result );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getLastImpactTime()
    {
        // float mLastImpactET
        
        return ( ByteUtil.readFloat( buffer, OFFSET_LAST_IMPACT_TIME ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getLastImpactMagnitude()
    {
        // float mLastImpactMagnitude
        
        return ( ByteUtil.readFloat( buffer, OFFSET_LAST_IMPACT_MAGNITUDE ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final TelemVect3 getLastImpactPosition( TelemVect3 lastImpactPos )
    {
        // TelemVect3 mLastImpactPos
        
        ByteUtil.readVectorF( buffer, OFFSET_LAST_IMPACT_POSITION, lastImpactPos );
        
        return ( lastImpactPos );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isSpeedLimiterOn()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isFrontFlapActivated()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isRearFlapActivated()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final DeviceLegalStatus getFrontFlapLegalStatus()
    {
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final DeviceLegalStatus getRearFlapLegalStatus()
    {
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final IgnitionStatus getIgnitionStatus()
    {
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getWheelRotation( Wheel wheel )
    {
        // float mRotation
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_WHEEL_ROTATION ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_WHEEL_ROTATION ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_WHEEL_ROTATION ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_WHEEL_ROTATION ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getWheelSuspensionDeflectionM( Wheel wheel )
    {
        // float mSuspensionDeflection
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_WHEEL_SUSPENSION_DEFLECTION ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_WHEEL_SUSPENSION_DEFLECTION ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_WHEEL_SUSPENSION_DEFLECTION ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_WHEEL_SUSPENSION_DEFLECTION ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getRideHeightM( Wheel wheel )
    {
        // float mRideHeight
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_RIDE_HEIGHT ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_RIDE_HEIGHT ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_RIDE_HEIGHT ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_RIDE_HEIGHT ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getTireLoadN( Wheel wheel )
    {
        // float mTireLoad
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_TIRE_LOAD ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_TIRE_LOAD ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_TIRE_LOAD ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_TIRE_LOAD ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getLateralForce( Wheel wheel )
    {
        // float mLateralForce
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_LATERAL_FORCE ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_LATERAL_FORCE ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_LATERAL_FORCE ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_LATERAL_FORCE ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getGripFraction( Wheel wheel )
    {
        // float mGripFract
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_GRIP_FRACTION ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_GRIP_FRACTION ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_GRIP_FRACTION ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_GRIP_FRACTION ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getBrakeTemperatureK( Wheel wheel )
    {
        // float mBrakeTemp
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_BRAKE_TEMP ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_BRAKE_TEMP ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_BRAKE_TEMP ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_BRAKE_TEMP ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getTirePressureKPa( Wheel wheel )
    {
        // float mPressure
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_TIRE_PRESSURE ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_TIRE_PRESSURE ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_TIRE_PRESSURE ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_TIRE_PRESSURE ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getTireTemperatureC( Wheel wheel, WheelPart part )
    {
        // float mTemperature[3], left/center/right (not to be confused with inside/center/outside!)
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_TIRE_TEMPERATURES + part.getArrayIndexFL() * ByteUtil.SIZE_FLOAT ) + Convert.ZERO_KELVIN );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_TIRE_TEMPERATURES + part.getArrayIndexFR() * ByteUtil.SIZE_FLOAT ) + Convert.ZERO_KELVIN );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_TIRE_TEMPERATURES + part.getArrayIndexRL() * ByteUtil.SIZE_FLOAT ) + Convert.ZERO_KELVIN );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_TIRE_TEMPERATURES + part.getArrayIndexRR() * ByteUtil.SIZE_FLOAT ) + Convert.ZERO_KELVIN );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    // TelemWheelV2
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final float getTireWear( Wheel wheel )
    {
        // float mWear
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_TIRE_WEAR ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_TIRE_WEAR ) );
            case REAR_LEFT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_TIRE_WEAR ) );
            case REAR_RIGHT:
                return ( ByteUtil.readFloat( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_TIRE_WEAR ) );
        }
        
        // Unreachable code!
        return ( 0f );
    }
    
    /**
     * Gets the material prefixes from the TDF file.
     * 
     * @param wheel the queried wheel
     * 
     * @return the material prefixes from the TDF file.
     */
    public final String getTerrainName( Wheel wheel )
    {
        // char mTerrainName[16]
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readString( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_TERRAIN_NAME, MAX_TERRAIN_NAME_LENGTH ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readString( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_TERRAIN_NAME, MAX_TERRAIN_NAME_LENGTH ) );
            case REAR_LEFT:
                return ( ByteUtil.readString( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_TERRAIN_NAME, MAX_TERRAIN_NAME_LENGTH ) );
            case REAR_RIGHT:
                return ( ByteUtil.readString( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_TERRAIN_NAME, MAX_TERRAIN_NAME_LENGTH ) );
        }
        
        // Unreachable code!
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final SurfaceType getSurfaceType( Wheel wheel )
    {
        // unsigned char mSurfaceType
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( SurfaceType.getFromIndex( ByteUtil.readUnsignedByte( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_SURFACE_TYPE ) ) );
            case FRONT_RIGHT:
                return ( SurfaceType.getFromIndex( ByteUtil.readUnsignedByte( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_SURFACE_TYPE ) ) );
            case REAR_LEFT:
                return ( SurfaceType.getFromIndex( ByteUtil.readUnsignedByte( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_SURFACE_TYPE ) ) );
            case REAR_RIGHT:
                return ( SurfaceType.getFromIndex( ByteUtil.readUnsignedByte( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_SURFACE_TYPE ) ) );
        }
        
        // Unreachable code!
        return ( null );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isWheelFlat( Wheel wheel )
    {
        // bool mFlat
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_FLAT ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_FLAT ) );
            case REAR_LEFT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_FLAT ) );
            case REAR_RIGHT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_FLAT ) );
        }
        
        // Unreachable code!
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isWheelDetached( Wheel wheel )
    {
        // bool mDetached
        
        switch ( wheel )
        {
            case FRONT_LEFT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 0 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_DETACHED ) );
            case FRONT_RIGHT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 1 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_DETACHED ) );
            case REAR_LEFT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 2 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_DETACHED ) );
            case REAR_RIGHT:
                return ( ByteUtil.readBoolean( buffer, OFFSET_WHEEL_DATA + 3 * WHEEL_DATA_SIZE + OFFSET_IS_WHEEL_DETACHED ) );
        }
        
        // Unreachable code!
        return ( false );
    }
    
    // future use
    
    // unsigned char mExpansion[32];
    
    _rf1_TelemetryData( LiveGameData gameData )
    {
        super( gameData );
        
        registerListener( _rf1_LifetimeManager.INSTANCE );
    }
}
