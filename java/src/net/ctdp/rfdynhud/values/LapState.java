/**
 * Copyright (C) 2009-2010 Cars and Tracks Development Project (CTDP).
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
package net.ctdp.rfdynhud.values;

import net.ctdp.rfdynhud.gamedata.Laptime;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;

public enum LapState
{
    OUTLAP( "OL" ),
    SOMEWHERE( "SW" ),
    AFTER_SECTOR1_START( "AS1S" ),
    BEFORE_SECTOR1_END( "BS1E" ),
    AFTER_SECTOR2_START( "AS2S" ),
    BEFORE_SECTOR2_END( "BS2E" ),
    AFTER_SECTOR3_START( "AS3S" ),
    BEFORE_SECTOR3_END( "BS3E" ),
    ;
    
    public final String SHORT;
    
    public final boolean isAfterSectorStart()
    {
        return ( ( this == AFTER_SECTOR1_START ) || ( this == AFTER_SECTOR2_START ) || ( this == AFTER_SECTOR3_START ) );
    }
    
    public final boolean isBeforeSectorEnd()
    {
        return ( ( this == BEFORE_SECTOR1_END ) || ( this == BEFORE_SECTOR2_END ) || ( this == BEFORE_SECTOR3_END ) );
    }
    
    private LapState( String SHORT )
    {
        this.SHORT = SHORT;
    }
    
    public static LapState getLapState( VehicleScoringInfo viewedVSI, Laptime refTime, float beforeSectorTime, float afterSectorTime, boolean firstSec1StartIsSomewhere )
    {
        if ( viewedVSI.getStintLength() < 1.0f )
            return ( OUTLAP );
        
        float laptime = viewedVSI.getCurrentLaptime();
        
        if ( ( refTime == null ) || !refTime.isFinished() )
        {
            if ( laptime < afterSectorTime )
            {
                if ( firstSec1StartIsSomewhere && ( viewedVSI.getStintLength() < 2.0f ) )
                    return ( SOMEWHERE );
                
                return ( AFTER_SECTOR1_START );
            }
            
            if ( ( viewedVSI.getStintLength() % 1.0f ) > 0.9f )
                return ( BEFORE_SECTOR3_END );
            
            return ( SOMEWHERE );
        }
        
        switch ( viewedVSI.getSector() )
        {
            case 1:
                if ( laptime < afterSectorTime )
                {
                    if ( firstSec1StartIsSomewhere && ( viewedVSI.getStintLength() < 2.0f ) )
                        return ( SOMEWHERE );
                    
                    return ( AFTER_SECTOR1_START );
                }
                
                if ( laptime < refTime.getSector1() - beforeSectorTime )
                    return ( SOMEWHERE );
                
                return ( BEFORE_SECTOR1_END );
                
            case 2:
                float sec1 = viewedVSI.getCurrentSector1();
                if ( laptime < sec1 + afterSectorTime )
                    return ( AFTER_SECTOR2_START );
                
                float gap1 = viewedVSI.getCurrentSector1() - refTime.getSector1();
                if ( laptime < refTime.getSector2( true ) + gap1 - beforeSectorTime )
                    return ( SOMEWHERE );
                
                return ( BEFORE_SECTOR2_END );
                
            case 3:
                float sec2 = viewedVSI.getCurrentSector2( true );
                if ( laptime < sec2 + afterSectorTime )
                    return ( AFTER_SECTOR3_START );
                
                float gap2 = viewedVSI.getCurrentSector2( true ) - refTime.getSector2( true );
                if ( laptime < refTime.getLapTime() + gap2 - beforeSectorTime )
                    return ( SOMEWHERE );
                
                return ( BEFORE_SECTOR3_END );
        }
        
        // Should be unreachable!
        return ( SOMEWHERE );
    }
}
