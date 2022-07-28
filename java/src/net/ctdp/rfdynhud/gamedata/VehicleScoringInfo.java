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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ctdp.rfdynhud.editor.EditorPresets;
import net.ctdp.rfdynhud.gamedata.ProfileInfo.SpeedUnits;
import net.ctdp.rfdynhud.util.RFDHLog;
import net.ctdp.rfdynhud.util.ThreeLetterCodeManager;

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
public abstract class VehicleScoringInfo
{
    protected final ScoringInfo scoringInfo;
    protected final ProfileInfo profileInfo;
    protected final LiveGameData gameData;
    
    private String oldDriverName = null;
    private String originalName = null;
    private String name = null;
    private String nameUC = null;
    private String nameShort = null;
    private String nameShortUC = null;
    private String nameTLC = null;
    private String nameTLCUC = null;
    private int nameId = 0;
    private Integer nameID = null;
    
    private short place = -1;
    
    private int lastTLCMgrUpdateId = -1;
    
    private String vehClass = null;
    private static int nextClassId = 1;
    private int classId = 0;
    private Integer classID = null;
    
    private String vehicleName = null;
    private VehicleInfo vehicleInfo = null;
    
    short placeByClass = -1;
    int numVehiclesInClass = -1;
    float timeBehindNextByClass = 0f;
    int lapsBehindNextByClass = -1;
    float timeBehindLeaderByClass = 0f;
    int lapsBehindLeaderByClass = -1;
    VehicleScoringInfo classLeaderVSI = null;
    VehicleScoringInfo classNextInFrontVSI = null;
    VehicleScoringInfo classNextBehindVSI = null;
    
    private float lapDistance = -1f;
    private int oldLap = -1;
    private int lap = -1;
    private int stintStartLap = -1;
    private float stintLength = 0f;
    private int pitState = -1;
    
    final List<Laptime> laptimes = new ArrayList<Laptime>();
    Laptime cachedFastestNormalLaptime = null;
    Laptime cachedFastestHotLaptime = null;
    private Laptime fastestLaptime = null;
    private Laptime secondFastestLaptime = null;
    Laptime oldAverageLaptime = null;
    Laptime averageLaptime = null;
    
    private Laptime editor_lastLaptime = null;
    private Laptime editor_currLaptime = null;
    private Laptime editor_fastestLaptime = null;
    
    float topspeed = 0f;
    
    float engineRPM = -1f;
    float engineMaxRPM = -1f;
    int engineBoostMapping = -1;
    int gear = -1000;
    
    private static final Map<String, Integer> classToIDMap = new HashMap<String, Integer>();
    
    public final ScoringInfo getScoringInfo()
    {
        return ( scoringInfo );
    }
    
    public boolean isValid()
    {
        return ( scoringInfo.isValid() );
    }
    
    protected void resetDerivateData()
    {
        this.oldDriverName = name;
        this.originalName = getDriverNameImpl();
        this.name = originalName;
        this.nameUC = null;
        this.nameShort = null;
        this.nameShortUC = null;
        this.nameTLC = null;
        this.nameTLCUC = null;
        
        this.classLeaderVSI = null;
        this.classNextInFrontVSI = null;
        this.classNextBehindVSI = null;
    }
    
    /**
     * Generates a <b>unique</b> ID for this vehicle.
     * 
     * @param index the current index into the list of {@link VehicleScoringInfo}s in {@link ScoringInfo}. This index may not stay valid after initialization.
     * 
     * @return a <b>unique</b> ID for this vehicle.
     */
    protected abstract Integer refreshIDImpl( int index );
    
    protected final Integer refreshID( int index )
    {
        this.nameID = refreshIDImpl( index );
        this.nameId = nameID.intValue();
        
        return ( nameID );
    }
    
    private void updateClassID()
    {
        String vehClass = getVehicleClass();
        
        Integer id = classToIDMap.get( vehClass );
        if ( id == null )
        {
            id = nextClassId++;
            classToIDMap.put( vehClass, id );
        }
        
        this.classId = id.intValue();
        this.classID = id;
    }
    
    void applyEditorPresets( int index, EditorPresets editorPresets )
    {
        if ( editorPresets == null )
            return;
        
        if ( isPlayer() )
        {
            oldDriverName = name;
            name = editorPresets.getDriverName();
            originalName = name;
            //setDriverName( name );
            nameUC = null;
            nameShort = null;
            nameShortUC = null;
            nameTLC = null;
            nameTLCUC = null;
            nameID = refreshID( index );
            nameId = nameID.intValue();
        }
        
        int lc = getLapsCompleted();
        if ( lc > 0 )
        {
            if ( ( laptimes.size() < lc ) || ( laptimes.get( lc - 1 ) == null ) || ( laptimes.get( lc - 1 ).getSector1() != editorPresets.getLastSector1Time() ) || ( laptimes.get( lc - 1 ).getSector2() != editorPresets.getLastSector2Time( false ) || ( laptimes.get( lc - 1 ).getSector3() != editorPresets.getLastSector3Time() ) ) )
            {
                fastestLaptime = null;
                secondFastestLaptime = null;
                java.util.Random rnd = new java.util.Random( System.nanoTime() );
                
                float ls1 = isPlayer() ? editorPresets.getLastSector1Time() : getLastSector1Impl();
                float ls2 = isPlayer() ? editorPresets.getLastSector2Time( false ) : getLastSector2Impl() - getLastSector1Impl();
                float ls3 = isPlayer() ? editorPresets.getLastSector3Time() : getLastLapTimeImpl() - getLastSector2Impl();
                
                for ( int l = 1; l <= lc; l++ )
                {
                    float s1 = ls1 + ( ( l == lc ) ? 0.0f : -0.33f * rnd.nextFloat() * 0.66f );
                    float s2 = ls2 + ( ( l == lc ) ? 0.0f : -0.33f * rnd.nextFloat() * 0.66f );
                    float s3 = ls3 + ( ( l == lc ) ? 0.0f : -0.33f * rnd.nextFloat() * 0.66f );
                    
                    Laptime lt;
                    if ( ( l > laptimes.size() ) || ( laptimes.get( l - 1 ) == null ) )
                    {
                        lt = new Laptime( getDriverId(), l, s1, s2, s3, false, l == 1, true );
                        if ( l > laptimes.size() )
                            laptimes.add( lt );
                        else
                            laptimes.set( l - 1, lt );
                    }
                    else
                    {
                        lt = laptimes.get( l - 1 );
                        lt.sector1 = s1;
                        lt.sector2 = s2;
                        lt.sector3 = s3;
                        lt.updateLaptimeFromSectors();
                    }
                    
                    if ( ( l == 1 ) || ( lt.getLapTime() < fastestLaptime.getLapTime() ) )
                    {
                        secondFastestLaptime = fastestLaptime;
                        fastestLaptime = lt;
                    }
                }
                
                editor_fastestLaptime = fastestLaptime;
                
                LaptimesRecorder.calcAvgLaptime( this );
                oldAverageLaptime = averageLaptime;
            }
        }
        
        float cs1 = isPlayer() ? editorPresets.getCurrentSector1Time() : getCurrentSector1Impl();
        float cs2 = isPlayer() ? editorPresets.getCurrentSector2Time( false ) : getCurrentSector2Impl() - getCurrentSector1Impl();
        
        if ( laptimes.size() < lc + 1 )
        {
            Laptime lt = new Laptime( getDriverId(), lc + 1, cs1, cs2, -1f, false, false, false );
            lt.isInLap = null;
            
            laptimes.add( lt );
        }
        else
        {
            Laptime lt = laptimes.get( lc );
            lt.sector1 = cs1;
            lt.sector2 = cs2;
            lt.updateLaptimeFromSectors();
        }
        
        if ( lc > 0 )
        {
            editor_lastLaptime = laptimes.get( lc - 1 );
            editor_currLaptime = laptimes.get( lc );
        }
        
        if ( getPlace( false ) > 0 )
            topspeed = editorPresets.getTopSpeed( getPlace( false ) - 1 );
    }
    
    /**
     * 
     * @param timestamp
     */
    protected void onDataUpdatedImpl( long timestamp )
    {
    }
    
    /**
     * 
     * @param timestamp
     */
    protected final void onDataUpdated( long timestamp )
    {
        try
        {
            this.originalName = getDriverNameImpl();
            
            place = -1;
            lapDistance = -1f;
            
            vehClass = null;
            classId = 0;
            classID = null;
            
            vehicleName = null;
            vehicleInfo = null;
            
            oldLap = lap;
            lap = getLapsCompleted() + 1;
            
            if ( isPlayer() && gameData.getTelemetryData().isUpdatedInTimeScope() )
            {
                engineRPM = gameData.getTelemetryData().getEngineRPM();
                engineMaxRPM = gameData.getTelemetryData().getEngineMaxRPM();
                engineBoostMapping = gameData.getTelemetryData().getEngineBoostMapping();
                gear = gameData.getTelemetryData().getCurrentGear();
            }
            else
            {
                engineRPM = -1f;
                engineMaxRPM = -1f;
                engineBoostMapping = -1;
                gear = -1000;
            }
            
            onDataUpdatedImpl( timestamp );
        }
        catch ( Throwable t )
        {
            RFDHLog.exception( t );
        }
    }
    
    private void updateStintLength()
    {
        int currentLap = getLapsCompleted() + 1; // Don't use getCurrentLap(), since it depends on stint length!
        boolean isInPits = isInPits();
        boolean isStanding = ( Math.abs( getScalarVelocityMPS() ) < 0.1f );
        float trackPos = getNormalizedLapDistance();
        
        if ( ( stintStartLap < 0 ) || ( isInPits && ( stintStartLap != currentLap ) && isStanding ) || ( stintStartLap > currentLap ) )
        {
            stintStartLap = currentLap;
        }
        
        int oldPitState = pitState;
        if ( oldPitState == -1 )
        {
            if ( isInPits && isStanding )
                pitState = 2;
            else if ( isInPits )
                pitState = 1;
            else
                pitState = 0;
        }
        else
        {
            if ( ( oldPitState == 2 ) && !isInPits )
            {
                stintStartLap = currentLap;
            }
            
            if ( isInPits )
            {
                if ( isStanding && ( oldPitState != 2 ) )
                    pitState = 2;
                else if ( oldPitState == 0 )
                    pitState = 1;
            }
            else if ( oldPitState != 0 )
            {
                pitState = 0;
            }
        }
        
        if ( !isPlayer() || gameData.isInCockpit() )
            stintLength = currentLap - stintStartLap + trackPos;
        else
            stintLength = 0.0f;
    }
    
    void updateSomeData()
    {
        updateStintLength();
    }
    
    void resetExtrapolatedValues()
    {
        lapDistance = -1f;
    }
    
    private void resetSessionDerivateData()
    {
        stintStartLap = -1;
        oldLap = -1;
        laptimes.clear();
        if ( laptimes.size() > 0 )
        {
            fastestLaptime = null;
            secondFastestLaptime = null;
        }
        oldAverageLaptime = null;
        averageLaptime = null;
        
        editor_lastLaptime = null;
        editor_currLaptime = null;
        editor_fastestLaptime = null;
    }
    
    void onSessionStarted()
    {
        resetSessionDerivateData();
        
        fastestLaptime = null;
        secondFastestLaptime = null;
    }
    
    void onSessionEnded()
    {
        resetSessionDerivateData();
        
        fastestLaptime = null;
        secondFastestLaptime = null;
    }
    
    protected final String getOldDriverName()
    {
        return ( oldDriverName );
    }
    
    protected abstract String getDriverNameImpl();
    
    protected final String getOriginalName()
    {
        return ( originalName );
    }
    
    /**
     * Gets the full name of the driver driving this vehicle.
     * 
     * @param upperCase whether the name should be in all upper case
     * 
     * @return the full name of the driver driving this vehicle.
     */
    public final String getDriverName( boolean upperCase )
    {
        /*
        if ( name == null )
        {
            name = getDriverNameImpl();
        }
        */
        
        if ( upperCase )
        {
            if ( nameUC == null )
                nameUC = name.toUpperCase();
            
            return ( nameUC );
        }
        
        return ( name );
    }
    
    /**
     * Gets the full name of the driver driving this vehicle.
     * 
     * @return the full name of the driver driving this vehicle.
     */
    public final String getDriverName()
    {
        return ( getDriverName( false ) );
    }
    
    /**
     * Gets driver name (short form)
     * 
     * @param upperCase whether the name should be in all upper case
     * 
     * @return driver name (short form)
     */
    public final String getDriverNameShort( boolean upperCase )
    {
        if ( ( nameShort == null ) || ( lastTLCMgrUpdateId < ThreeLetterCodeManager.getUpdateId() ) )
        {
            //String driverName = getDriverName( false );
            String driverName = originalName;
            nameShort = ThreeLetterCodeManager.getShortForm( driverName, getDriverID(), scoringInfo.getThreeLetterCodeGenerator() );
            lastTLCMgrUpdateId = ThreeLetterCodeManager.getUpdateId();
        }
        
        if ( upperCase )
        {
            if ( nameShortUC == null )
                nameShortUC = nameShort.toUpperCase();
            
            return ( nameShortUC );
        }
        
        return ( nameShort );
    }
    
    /**
     * Gets driver name (short form)
     * 
     * @return driver name (short form)
     */
    public final String getDriverNameShort()
    {
        return ( getDriverNameShort( false ) );
    }
    
    /**
     * Gets driver name (three letter code)
     * 
     * @param upperCase whether the name should be in all upper case
     * 
     * @return driver name (three letter code)
     */
    public final String getDriverNameTLC( boolean upperCase )
    {
        if ( ( nameTLC == null ) || ( lastTLCMgrUpdateId < ThreeLetterCodeManager.getUpdateId() ) )
        {
            //String driverName = getDriverName( false );
            String driverName = originalName;
            nameTLC = ThreeLetterCodeManager.getThreeLetterCode( driverName, getDriverID(), scoringInfo.getThreeLetterCodeGenerator() );
            lastTLCMgrUpdateId = ThreeLetterCodeManager.getUpdateId();
        }
        
        if ( upperCase )
        {
            if ( nameTLCUC == null )
                nameTLCUC = nameTLC.toUpperCase();
            
            return ( nameTLCUC );
        }
        
        return ( nameTLC );
    }
    
    /**
     * Gets driver name (three letter code)
     * 
     * @return driver name (three letter code)
     */
    public final String getDriverNameTLC()
    {
        return ( getDriverNameTLC( true ) );
    }
    
    /**
     * Uniquely identifes this vehicle's driver. It returns the same value as {@link #getDriverID()}, but as a primitive int.
     * 
     * @return the driver's id.
     */
    public final int getDriverId()
    {
        return ( nameId );
    }
    
    /**
     * Uniquely identifes this vehicle's driver. It returns the same value as {@link #getDriverId()}, but as an {@link Integer} instance.
     * 
     * @return the driver's id.
     */
    public final Integer getDriverID()
    {
        return ( nameID );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return ( getDriverId() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object o )
    {
        if ( !( o instanceof VehicleScoringInfo ) )
            return ( false );
        
        return ( this.getDriverId() == ( (VehicleScoringInfo)o ).getDriverId() );
    }
    
    /**
     * Gets the vehicle's name.
     * 
     * @return the vehicle's name.
     */
    protected abstract String getVehicleNameImpl();
    
    /**
     * Gets the vehicle's name.
     * 
     * @return the vehicle's name.
     */
    public final String getVehicleName()
    {   
        if ( vehicleName == null )
        {
            vehicleName = getVehicleNameImpl();
        }
        
        return ( vehicleName );
    }
    
    public final VehicleInfo getVehicleInfo()
    {
        if ( vehicleInfo == null )
        {
            vehicleInfo = gameData.getModInfo().getVehicleInfoForDriver( this );
        }
        
        return ( vehicleInfo );
    }
    
    /**
     * Gets the number of laps completed.
     * 
     * @return the number of laps completed.
     */
    public abstract short getLapsCompleted();
    
    /**
     * Gets the current lap index (one based).
     * 
     * @return the current lap index (one based)
     */
    public final short getCurrentLap()
    {
        if ( isInPits() && ( getStintLength() < 0.5f ) )
            return ( getLapsCompleted() );
        
        return ( (short)( getLapsCompleted() + 1 ) );
    }
    
    /**
     * Gets, whether the current lap has just been stared. This is <code>true</code> for just one single time at the beginning of the lap.
     * 
     * @return whether the current lap has just been stared
     */
    public final boolean isLapJustStarted()
    {
        return ( lap != oldLap );
    }
    
    /**
     * Gets the {@link SessionLimit} of the current session. If the session limit is defined to be LAPS,
     * LAPS is returned. If it is defined to be timed, TIME is returned. Otherwise the method
     * tries to guess the limit based on the average laptime.
     * 
     * @param preference if both TIME and LAPS are possible, preference is returned.
     * 
     * @return the {@link SessionLimit}.
     */
    public final SessionLimit getSessionLimit( SessionLimit preference )
    {
        int maxLaps = scoringInfo.getMaxLaps();
        if ( maxLaps > Integer.MAX_VALUE / 2 )
            maxLaps = 0;
        final float endTime = scoringInfo.getEndTime();
        
        if ( ( maxLaps > 0 ) && ( maxLaps < 10000 ) )
        {
            if ( ( endTime > 0f ) && ( endTime < 999999f ) )
            {
                Laptime avgLaptime = getAverageLaptime();
                
                if ( avgLaptime == null )
                {
                    if ( preference == null )
                        return ( SessionLimit.LAPS );
                    
                    return ( preference );
                }
                
                int timeLaps = (int)( endTime / avgLaptime.getLapTime() );
                
                if ( timeLaps < maxLaps )
                    return ( SessionLimit.TIME );
            }
            
            return ( SessionLimit.LAPS );
        }
        
        if ( ( endTime > 0f ) && ( endTime < 999999f ) )
            return ( SessionLimit.TIME );
        
        return ( null );
    }
    
    /**
     * Gets the {@link SessionLimit} of the current session. If the session limit is defined to be LAPS,
     * LAPS is returned. If it is defined to be timed, TIME is returned. Otherwise the method
     * tries to guess the limit based on the average laptime.
     * 
     * @return the {@link SessionLimit}.
     */
    public final SessionLimit getSessionLimit()
    {
        return ( getSessionLimit( null ) );
    }
    
    /**
     * Gets the estimated max laps based on the session end time and average lap time.
     * If the {@link SessionLimit} is defined to be LAPS, then max laps is known and returned.
     * 
     * @return the estimated max laps.
     */
    public final int getEstimatedMaxLaps()
    {
        if ( scoringInfo.getSessionType().isRace() && scoringInfo.getLeadersVehicleScoringInfo().getFinishStatus().isFinished() )
        {
            return ( scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() );
        }
        
        short lapsCompleted = getLapsCompleted();
        int maxLaps = scoringInfo.getMaxLaps();
        if ( maxLaps > Integer.MAX_VALUE / 2 )
            maxLaps = 0;
        float endTime = scoringInfo.getEndTime();
        if ( ( lapsCompleted == 0 ) || ( endTime < 0f ) || ( endTime > 999999f ) )
        {
            if ( maxLaps > 0 )
                return ( maxLaps );
            
            return ( -1 );
        }
        
        Laptime avgLaptime = getAverageLaptime();
        if ( avgLaptime == null )
        {
            if ( maxLaps > 0 )
                return ( maxLaps );
            
            return ( -1 );
        }
        
        float restTime = endTime - getLapStartTime();
        int timeLaps = lapsCompleted + (int)( restTime / avgLaptime.getLapTime() ) + 1;
        
        if ( ( maxLaps <= 0 ) || ( timeLaps < maxLaps ) )
            return ( timeLaps );
        
        return ( maxLaps );
    }
    
    /**
     * Gets the number of remaining laps (with fractions).
     * 
     * @param maxLaps the maximum laps in the race
     * 
     * @return the number of remaining laps.
     */
    public final float getLapsRemaining( int maxLaps )
    {
        if ( maxLaps < 0 )
            return ( -1f );
        
        int lr = maxLaps - getLapsCompleted();
        
        if ( getFinishStatus().isFinished() )
            return ( lr );
        
        return ( lr - getNormalizedLapDistance() );
    }
    
    /**
     * Gets the current sector (1,2,3).
     * 
     * @return the current sector.
     */
    public abstract byte getSector();
    
    /**
     * Gets the current finish status.
     * 
     * @return the current finish status.
     */
    public abstract FinishStatus getFinishStatus();
    
    /**
     * Gets the current distance around track in meters at the time of the last data update.
     * 
     * @return the current distance around track in meters.
     */
    protected abstract float getLastKnownLapDistance();
    
    /**
     * Gets the current distance around track in meters.
     * 
     * @return the current distance around track in meters.
     */
    public final float getLapDistance()
    {
        if ( lapDistance < 0f )
        {
            //lapDistance = ( data.getLapDistance() + getScalarVelocityMPS() * scoringInfo.getExtrapolationTime() ) % scoringInfo.getTrackLength();
            
            lapDistance = getLastKnownLapDistance() + getScalarVelocityMPS() * scoringInfo.getExtrapolationTime();
            
            while ( lapDistance < 0f )
                lapDistance += scoringInfo.getTrackLength();
            
            lapDistance %= scoringInfo.getTrackLength();
        }
        
        return ( lapDistance );
    }
    
    /**
     * Gets current distance around track as a fraction [0,1].
     * 
     * @return current distance around track as a fraction [0,1].
     */
    public final float getNormalizedLapDistance()
    {
        return ( getLapDistance() / scoringInfo.getTrackLength() );
    }
    
    /**
     * Gets the lap, at which we started the current stint.
     * 
     * @return the lap, at which we started the current stint.
     */
    public final int getStintStartLap()
    {
        return ( stintStartLap );
    }
    
    /**
     * Gets the current stint length with fractions.
     * 
     * @return the current stint length with fractions.
     */
    public final float getStintLength()
    {
        return ( stintLength );
    }
    
    /**
     * Gets the Laptime object for the given lap.
     * 
     * @param lap the lap
     * 
     * @return the Laptime object for the given lap.
     */
    public final Laptime getLaptime( int lap )
    {
        if ( ( lap < 1 ) || ( laptimes == null ) || ( lap > laptimes.size() ) )
            return ( null );
        
        return ( laptimes.get( lap - 1 ) );
    }
    
    void setFastestLaptime( Laptime laptime )
    {
        if ( laptime == this.fastestLaptime )
            return;
        
        if ( ( laptime == null ) || !laptime.isFinished() || ( laptime.getLapTime() < 0f ) )
            this.secondFastestLaptime = null;
        else
            this.secondFastestLaptime = this.fastestLaptime;
        this.fastestLaptime = laptime;
    }
    
    final Laptime _getFastestLaptime()
    {
        return ( fastestLaptime );
    }
    
    /**
     * Gets this driver's fastest {@link Laptime}.
     * 
     * @return this driver's fastest {@link Laptime}.
     */
    public final Laptime getFastestLaptime()
    {
        if ( isPlayer() && DataCache.checkSessionType( scoringInfo ) )
        {
            Laptime cached = Laptime.isHotlap( gameData ) ? cachedFastestHotLaptime : cachedFastestNormalLaptime;
            
            if ( ( cached != null ) && ( ( fastestLaptime == null ) || ( cached.getLapTime() < fastestLaptime.getLapTime() ) ) )
                return ( cached );
        }
        
        return ( fastestLaptime );
    }
    
    /**
     * Gets this driver's 2nd fastest {@link Laptime}.
     * 
     * @return this driver's 2nd fastest {@link Laptime}.
     */
    public final Laptime getSecondFastestLaptime()
    {
        return ( secondFastestLaptime );
    }
    
    /**
     * Gets the average laptime of the current session excluding the last timed lap. Inlaps, outlaps and laps slower than (1.06 * fastest) are ignored.
     * 
     * @return the average laptime or -1.
     */
    public final Laptime getOldAverageLaptime()
    {
        return ( oldAverageLaptime );
    }
    
    /**
     * Gets the average laptime of the current session. Inlaps, outlaps and laps slower than (1.06 * fastest) are ignored.
     * 
     * @return the average laptime or -1.
     */
    public final Laptime getAverageLaptime()
    {
        return ( averageLaptime );
    }
    
    /**
     * Gets lateral position with respect to *very approximate* "center" path.
     * 
     * @return lateral position with respect to *very approximate* "center" path.
     */
    public abstract float getPathLateral();
    
    /**
     * Gets track edge (w.r.t. "center" path) on same side of track as vehicle.
     * 
     * @return track edge (w.r.t. "center" path) on same side of track as vehicle.
     */
    public abstract float getTrackEdge();
    
    /**
     * Gets the best sector 1 time. This is not necessarily the sector time of the best lap.
     * 
     * @return the best sector 1 time.
     */
    protected abstract float getBestSector1Impl();
    
    /**
     * Gets the best sector 1 time. This is not necessarily the sector time of the best lap.
     * 
     * @return the best sector 1 time.
     */
    public final float getBestSector1()
    {
        if ( editor_fastestLaptime != null )
            return ( editor_fastestLaptime.getSector1() );
        
        return ( getBestSector1Impl() );
    }
    
    /**
     * Gets the best sector 2 time including sector1. This is not necessarily the sector time of the best lap.
     * 
     * @return the best sector 2 time.
     */
    protected abstract float getBestSector2Impl();
    
    /**
     * Gets the best sector 2 time. This is not necessarily the sector time of the best lap.
     * 
     * @param includingSector1 return sum of sector1 and 2?
     * 
     * @return the best sector 2 time.
     */
    public final float getBestSector2( boolean includingSector1 )
    {
        if ( editor_fastestLaptime != null )
            return ( editor_fastestLaptime.getSector2( includingSector1 ) );
        
        float sec2 = getBestSector2Impl();
        
        if ( !includingSector1 && ( sec2 > 0f ) )
            sec2 -= getBestSector1();
        
        return ( sec2 );
    }
    
    /**
     * Gets the best lap time best lap time.
     * 
     * @return the best lap time best lap time.
     */
    protected abstract float getBestLapTimeImpl();
    
    /**
     * Gets the best lap time best lap time.
     * 
     * @return the best lap time best lap time.
     */
    public final float getBestLapTime()
    {
        if ( editor_fastestLaptime != null )
            return ( editor_fastestLaptime.getLapTime() );
        
        return ( getBestLapTimeImpl() );
    }
    
    /**
     * Gets the best sector 3 time excluding sector 1 and 2. This is not necessarily the sector time of the best lap.
     * 
     * @return the best sector 3 time.
     */
    public float getBestSector3()
    {
        if ( editor_fastestLaptime != null )
            return ( editor_fastestLaptime.getSector3() );
        
        float lt = getBestLapTime();
        if ( lt > 0f )
            lt -= getBestSector2( true );
        
        return ( lt );
    }
    
    /**
     * Gets the last sector 1 time.
     * 
     * @return the last sector 1 time.
     */
    protected abstract float getLastSector1Impl();
    
    /**
     * Gets the last sector 1 time.
     * 
     * @return the last sector 1 time.
     */
    public final float getLastSector1()
    {
        if ( editor_lastLaptime != null )
            return ( editor_lastLaptime.getSector1() );
        
        return ( getLastSector1Impl() );
    }
    
    /**
     * Gets the last sector 2 time including sector 1.
     * 
     * @return the last sector 2 time.
     */
    protected abstract float getLastSector2Impl();
    
    /**
     * Gets the last sector 2 time.
     * 
     * @param includingSector1 return sum of sector1 and 2?
     * 
     * @return the last sector 2 time.
     */
    public final float getLastSector2( boolean includingSector1 )
    {
        if ( editor_lastLaptime != null )
            return ( editor_lastLaptime.getSector2( includingSector1 ) );
        
        float sec2 = getLastSector2Impl();
        
        if ( !includingSector1 )
            sec2 -= getLastSector1();
        
        return ( sec2 );
    }
    
    /**
     * Gets the last lap time.
     * 
     * @return the last lap time.
     */
    protected abstract float getLastLapTimeImpl();
    
    /**
     * Gets the last lap time.
     * 
     * @return the last lap time.
     */
    public final float getLastLapTime()
    {
        if ( editor_lastLaptime != null )
            return ( editor_lastLaptime.getLapTime() );
        
        return ( getLastLapTimeImpl() );
    }
    
    /**
     * Gets the last lap time.
     * 
     * @return the last lap time.
     */
    public final Laptime getLastLaptime()
    {
        if ( editor_lastLaptime != null )
            return ( editor_lastLaptime );
        
        return ( getLaptime( getLapsCompleted() ) );
    }
    
    /**
     * Gets the last sector 3 time.
     * 
     * @return the last sector 3 time.
     */
    public final float getLastSector3()
    {
        if ( editor_lastLaptime != null )
            return ( editor_lastLaptime.getSector3() );
        
        return ( getLastLapTime() - getLastSector2( true ) );
    }
    
    /**
     * Gets the current sector 1 (if valid).
     * 
     * @return the current sector 1 (if valid)
     */
    protected abstract float getCurrentSector1Impl();
    
    /**
     * Gets the current sector 1 (if valid).
     * 
     * @return the current sector 1 (if valid)
     */
    public final float getCurrentSector1()
    {
        if ( editor_currLaptime != null )
            return ( editor_currLaptime.getSector1() );
        
        return ( getCurrentSector1Impl() );
    }
    
    /**
     * Gets current sector 2 time including sector 1.
     * 
     * @return current sector 2 time.
     */
    protected abstract float getCurrentSector2Impl();
    
    /**
     * Gets current sector 2 time.
     * 
     * @param includingSector1 only affects result if sector1 is valid
     * 
     * @return current sector 2 time.
     */
    public final float getCurrentSector2( boolean includingSector1 )
    {
        if ( editor_currLaptime != null )
            return ( editor_currLaptime.getSector2( includingSector1 ) );
        
        float sec2 = getCurrentSector2Impl();
        
        if ( !includingSector1 && ( sec2 > 0f ) )
            sec2 -= getCurrentSector1();
        
        return ( sec2 );
    }
    
    /**
     * The current laptime (may be incomplete).
     * 
     * @return current laptime.
     */
    public final float getCurrentLaptime()
    {
        /*
        if ( getStintLength() < 1.0f )
            return ( -1f );
        */
        
        if ( !scoringInfo.getSessionType().isRace() && ( getStintLength() < 1.0f ) )
            return ( -1f );
        
        return ( scoringInfo.getSessionTime() - getLapStartTime() );
    }
    
    /**
     * Gets the number of pitstops made.
     * 
     * @return the number of pitstops made.
     */
    public abstract short getNumPitstopsMade();
    
    /**
     * Gets the number of scheduled pitstops (only valid for the player).
     * 
     * @return the number of scheduled pitstops.
     */
    public short getNumberOfScheduledPitstops()
    {
        if ( !isPlayer() )
            return ( -1 );
        
        return ( gameData.getTelemetryData().getNumberOfScheduledPitstops() );
    }
    
    /**
     * Gets the number of outstanding penalties.
     * 
     * @return the number of outstanding penalties.
     */
    public abstract short getNumOutstandingPenalties();
    
    /**
     * @return is this the player's vehicle?
     */
    public abstract boolean isPlayer();
    
    /**
     * @return who's in control?
     */
    public abstract VehicleControl getVehicleControl();
    
    /**
     * between pit entrance and pit exit (not always accurate for remote vehicles)
     * 
     * @return is this vehicle in the pit lane?
     */
    public abstract boolean isInPits();
    
    /**
     * Gets the number of vehicles in the same vehicle class.
     * 
     * @return the number of vehicles in the same vehicle class.
     */
    public final int getNumVehiclesInSameClass()
    {
        scoringInfo.updateClassScoring();
        
        return ( numVehiclesInClass );
    }
    
    /**
     * 1-based position
     * 
     * @return 1-based position
     */
    protected abstract short getPlaceImpl();
    
    /**
     * 1-based position
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return 1-based position
     */
    public final short getPlace( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( placeByClass );
        }
        
        if ( place < 0 )
        {
            place = getPlaceImpl();
        }
        
        return ( place );
    }
    
    /**
     * Gets the {@link VehicleScoringInfo}, that leads the same class.
     * 
     * @return the {@link VehicleScoringInfo}, that leads the same class.
     */
    public final VehicleScoringInfo getLeaderByClass()
    {
        scoringInfo.updateClassScoring();
        
        return ( classLeaderVSI );
    }
    
    /**
     * Gets the {@link VehicleScoringInfo}, that is next in front.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the {@link VehicleScoringInfo}, that is next in front.
     */
    public final VehicleScoringInfo getNextInFront( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( classNextInFrontVSI );
        }
        
        short place = getPlace( false );
        
        if ( place <= 1 )
            return ( null );
        
        return ( scoringInfo.getVehicleScoringInfo( place - 2 ) );
    }
    
    /**
     * Gets the {@link VehicleScoringInfo}, that is next behind.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the {@link VehicleScoringInfo}, that is next behind.
     */
    public final VehicleScoringInfo getNextBehind( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( classNextBehindVSI );
        }
        
        short place = getPlace( false );
        
        if ( place >= scoringInfo.getNumVehicles() )
            return ( null );
        
        return ( scoringInfo.getVehicleScoringInfo( place + 0 ) );
    }
    
    /**
     * Gets the vehicle class.
     * 
     * @return the vehicle class.
     */
    protected abstract String getVehicleClassImpl();
    
    /**
     * Gets the vehicle class.
     * 
     * @return the vehicle class.
     */
    public final String getVehicleClass()
    {
        if ( vehClass == null )
        {
            vehClass = getVehicleClassImpl();
        }
        
        return ( vehClass );
    }
    
    void setVehicleClass( String vehClass )
    {
        this.vehClass = vehClass;
    }
    
    /**
     * Gets the vehicle class id.
     * 
     * @return the vehicle class id.
     */
    public final int getVehicleClassId()
    {
        if ( classId <= 0 )
        {
            updateClassID();
        }
        
        return ( classId );
    }
    
    /**
     * Gets the vehicle class id.
     * 
     * @return the vehicle class id.
     */
    public final Integer getVehicleClassID()
    {
        if ( classID == null )
        {
            updateClassID();
        }
        
        return ( classID );
    }
    
    /**
     * Gets the time behind vehicle in next higher place.
     * 
     * @return the time behind vehicle in next higher place.
     */
    protected abstract float getTimeBehindNextInFrontImpl();
    
    /**
     * Gets the time behind vehicle in next higher place.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the time behind vehicle in next higher place.
     */
    public final float getTimeBehindNextInFront( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( timeBehindNextByClass );
        }
        
        return ( getTimeBehindNextInFrontImpl() );
    }
    
    /**
     * Gets the laps behind vehicle in next higher place.
     * 
     * @return the laps behind vehicle in next higher place.
     */
    protected abstract int getLapsBehindNextInFrontImpl();
    
    /**
     * Gets the laps behind vehicle in next higher place.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the laps behind vehicle in next higher place.
     */
    public final int getLapsBehindNextInFront( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( lapsBehindNextByClass );
        }
        
        return ( getLapsBehindNextInFrontImpl() );
    }
    
    /**
     * Gets the time behind leader.
     * 
     * @return the time behind leader.
     */
    protected abstract float getTimeBehindLeaderImpl();
    
    /**
     * Gets the time behind leader.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the time behind leader.
     */
    public final float getTimeBehindLeader( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( timeBehindLeaderByClass );
        }
        
        return ( getTimeBehindLeaderImpl() );
    }
    
    /**
     * Gets the laps behind leader.
     * 
     * @return the laps behind leader.
     */
    protected abstract int getLapsBehindLeaderImpl();
    
    /**
     * Gets the laps behind leader.
     * 
     * @param byClass only consider vehicles in the same class
     * 
     * @return the laps behind leader.
     */
    public final int getLapsBehindLeader( boolean byClass )
    {
        if ( byClass )
        {
            scoringInfo.updateClassScoring();
            
            return ( lapsBehindLeaderByClass );
        }
        
        return ( getLapsBehindNextInFrontImpl() );
    }
    
    /**
     * Gets the time this lap was started at.
     * 
     * @return the time this lap was started at.
     */
    public abstract float getLapStartTime();
    
    /**
     * Gets world position in meters.
     * 
     * @param position output buffer
     */
    public abstract void getWorldPosition( TelemVect3 position );
    
    /**
     * Gets world position in meters.
     * 
     * @return world position in meters.
     */
    public abstract float getWorldPositionX();
    
    /**
     * Gets world position in meters.
     * 
     * @return world position in meters.
     */
    public abstract float getWorldPositionY();
    
    /**
     * Gets world position in meters.
     * 
     * @return world position in meters.
     */
    public abstract float getWorldPositionZ();
    
    /**
     * Gets the current engine RPM.<br />
     * This is only valid, if set by a {@link LiveGameDataController}.
     * 
     * @return the current engine RPM or -1, if unknown.
     */
    public final float getEngineRPM()
    {
        return ( engineRPM );
    }
    
    /**
     * Gets the current engine' max RPM.<br />
     * This is only valid, if set by a {@link LiveGameDataController}.
     * 
     * @return the current engine max RPM or -1, if unknown.
     */
    public final float getEngineMaxRPM()
    {
        return ( engineMaxRPM );
    }
    
    /**
     * Gets the current engine boost mapping.<br />
     * This is only valid, if set by a {@link LiveGameDataController}.
     * 
     * @return the current engine boost mapping or -1, if unknown.
     */
    public final int getEngineBoostMapping()
    {
        return ( engineBoostMapping );
    }
    
    /**
     * Gets the current gear.<br />
     * This is only valid, if set by a {@link LiveGameDataController}.
     * 
     * @return the current gear or -1000, if unknown.
     */
    public int getCurrentGear()
    {
        return ( gear );
    }
    
    /**
     * velocity (meters/sec) in local vehicle coordinates
     * 
     * @param localVel output buffer
     */
    public abstract void getLocalVelocity( TelemVect3 localVel );
    
    /**
     * Gets velocity (meters/sec) in local vehicle coordinates.
     * 
     * @return velocity (meters/sec) in local vehicle coordinates.
     */
    public abstract float getScalarVelocityMS();
    
    /**
     * @deprecated replaced by {@link #getScalarVelocityMS()}.
     * 
     * @return velocity (meters/sec) in local vehicle coordinates.
     */
    @Deprecated
    public final float getScalarVelocityMPS()
    {
        return ( getScalarVelocityMS() );
    }
    
    /**
     * Gets velocity (mi/h).
     * 
     * @return velocity (mi/h).
     */
    public final float getScalarVelocityMih()
    {
        float mps = getScalarVelocityMS();
        
        return ( mps * SpeedUnits.Convert.MS_TO_MIH );
    }
    
    /**
     * @deprecated replaced by {@link #getScalarVelocityMih()}.
     * 
     * @return velocity (mi/h).
     */
    @Deprecated
    public final float getScalarVelocityMPH()
    {
        return ( getScalarVelocityMih() );
    }
    
    /**
     * Gets velocity (km/h).
     * 
     * @return velocity (km/h).
     */
    public final float getScalarVelocityKmh()
    {
        float mps = getScalarVelocityMS();
        
        return ( mps * SpeedUnits.Convert.MS_TO_KMH );
    }
    
    /**
     * @deprecated replaced by {@link #getScalarVelocityKmh()}.
     * 
     * @return velocity (km/h).
     */
    @Deprecated
    public final float getScalarVelocityKPH()
    {
        return ( getScalarVelocityKmh() );
    }
    
    /**
     * Gets velocity in the units selected in the PLR.
     * 
     * @return velocity in the units selected in the PLR.
     */
    public final float getScalarVelocity()
    {
        if ( profileInfo.getSpeedUnits() == SpeedUnits.MIH )
            return ( getScalarVelocityMih() );
        
        return ( getScalarVelocityKmh() );
    }
    
    /**
     * Gets topspeed in km/h.
     * 
     * @return topspeed in km/h.
     */
    public final float getTopspeed()
    {
        return ( topspeed );
    }
    
    /**
     * acceleration (meters/sec^2) in local vehicle coordinates
     * 
     * @param localAccel output buffer
     */
    public abstract void getLocalAcceleration( TelemVect3 localAccel );
    
    /**
     * top row of orientation matrix (also converts local vehicle vectors into world X using dot product)
     * 
     * @param oriX output buffer
     */
    public abstract void getOrientationX( TelemVect3 oriX );
    
    /**
     * mid row of orientation matrix (also converts local vehicle vectors into world Y using dot product)
     * 
     * @param oriY output buffer
     */
    public abstract void getOrientationY( TelemVect3 oriY );
    
    /**
     * bot row of orientation matrix (also converts local vehicle vectors into world Z using dot product)
     * 
     * @param oriZ output buffer
     */
    public abstract void getOrientationZ( TelemVect3 oriZ );
    
    /**
     * rotation (radians/sec) in local vehicle coordinates
     * 
     * @param localRot output buffer
     */
    public abstract void getLocalRotation( TelemVect3 localRot );
    
    /**
     * rotational acceleration (radians/sec^2) in local vehicle coordinates
     * 
     * @param localRotAccel output buffer
     */
    public abstract void getLocalRotationalAcceleration( TelemVect3 localRotAccel );
    
    /**
     * Gets the current vehicle's pit state.
     * 
     * @return the current vehicle's pit state or <code>null</code>, if unknown.
     */
    public abstract PitState getPitState();
    
    /**
     * Gets the status flag, currently shown to this vehicle.
     * 
     * @return the status flag, currently shown to this vehicle or <code>null</code>, if unknown.
     */
    public abstract StatusFlag getStatusFlag();
    
    /**
     * Gets a description of the currently installed vehicle upgrade pack.
     * 
     * @return a description of the currently installed vehicle upgrade pack or <code>null</code>, if unknown.
     */
    public abstract VehicleUpgradePack getUpgradePack();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if ( !isValid() )
            return ( this.getClass().getSimpleName() + " (invalid)" );
        
        return ( this.getClass().getSimpleName() + " (\"" + getDriverName() + "\", " + getDriverId() + ")" );
    }
    
    protected VehicleScoringInfo( ScoringInfo scoringInfo, ProfileInfo profileInfo, LiveGameData gameData )
    {
        this.scoringInfo = scoringInfo;
        this.profileInfo = profileInfo;
        this.gameData = gameData;
    }
    
    /**
     * Comparator to sort by place.
     * 
     * @author Marvin Froehlich
     */
    public static final class VSIPlaceComparator implements Comparator<VehicleScoringInfo>
    {
        /**
         * Singleton instance
         */
        public static final VSIPlaceComparator INSTANCE = new VSIPlaceComparator();
        
        @Override
        public int compare( VehicleScoringInfo vsi1, VehicleScoringInfo vsi2 )
        {
            if ( vsi1.getPlace( false ) < vsi2.getPlace( false ) )
                return ( -1 );
            
            if ( vsi1.getPlace( false ) > vsi2.getPlace( false ) )
                return ( +1 );
            
            return ( 0 );
        }
        
        private VSIPlaceComparator()
        {
        }
    }
}
