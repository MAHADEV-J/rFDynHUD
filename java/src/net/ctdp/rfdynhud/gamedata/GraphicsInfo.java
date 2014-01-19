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

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
public abstract class GraphicsInfo
{
    private long updateTimestamp = -1L;
    private long updateId = 0L;
    
    protected final LiveGameData gameData;
    
    private boolean updatedInRealtimeMode = false;
    
    public static interface GraphicsInfoUpdateListener extends LiveGameData.GameDataUpdateListener
    {
        public void onViewportChanged( LiveGameData gameData, int viewportX, int viewportY, int viewportWidth, int viewportHeight );
        
        public void onGraphicsInfoUpdated( LiveGameData gameData, boolean isEditorMode );
    }
    
    private GraphicsInfoUpdateListener[] updateListeners = null;
    
    public void registerListener( GraphicsInfoUpdateListener l )
    {
        if ( updateListeners == null )
        {
            updateListeners = new GraphicsInfoUpdateListener[] { l };
        }
        else
        {
            for ( int i = 0; i < updateListeners.length; i++ )
            {
                if ( updateListeners[i] == l )
                    return;
            }
            
            GraphicsInfoUpdateListener[] tmp = new GraphicsInfoUpdateListener[ updateListeners.length + 1 ];
            System.arraycopy( updateListeners, 0, tmp, 0, updateListeners.length );
            updateListeners = tmp;
            updateListeners[updateListeners.length - 1] = l;
        }
        
        gameData.registerDataUpdateListener( l );
    }
    
    public void unregisterListener( GraphicsInfoUpdateListener l )
    {
        if ( updateListeners == null )
            return;
        
        int index = -1;
        for ( int i = 0; i < updateListeners.length; i++ )
        {
            if ( updateListeners[i] == l )
            {
                index = i;
                break;
            }
        }
        
        if ( index < 0 )
            return;
        
        if ( updateListeners.length == 1 )
        {
            updateListeners = null;
            return;
        }
        
        GraphicsInfoUpdateListener[] tmp = new GraphicsInfoUpdateListener[ updateListeners.length - 1 ];
        if ( index > 0 )
            System.arraycopy( updateListeners, 0, tmp, 0, index );
        if ( index < updateListeners.length - 1 )
            System.arraycopy( updateListeners, index + 1, tmp, index, updateListeners.length - index - 1 );
        updateListeners = tmp;
        
        gameData.unregisterDataUpdateListener( l );
    }
    
    /**
     * Gets the system nano time for the last data update.
     * 
     * @return the system nano time for the last data update.
     */
    public final long getUpdateTimestamp()
    {
        return ( updateTimestamp );
    }
    
    /**
     * This is incremented every time the info is updated.
     *  
     * @return the current update id.
     */
    public final long getUpdateId()
    {
        return ( updateId );
    }
    
    /**
     * 
     * @param userObject
     * @param timestamp
     */
    protected void prepareDataUpdate( Object userObject, long timestamp )
    {
    }
    
    /**
     * Increments the update ID.
     * 
     * @param userObject
     * @param timestamp
     */
    protected void onDataUpdated( Object userObject, long timestamp )
    {
        this.updateTimestamp = timestamp;
        this.updateId++;
    }
    
    protected void onDataUpdated( Object userObject, long timestamp, boolean isEditorMode )
    {
        this.updatedInRealtimeMode = gameData.isInRealtimeMode();
        onDataUpdated( userObject, timestamp );
        
        if ( updateListeners != null )
        {
            for ( int i = 0; i < updateListeners.length; i++ )
                updateListeners[i].onGraphicsInfoUpdated( gameData, isEditorMode );
        }
    }
    
    protected abstract void updateDataImpl( Object userObject, long timestamp );
    
    protected void updateData( Object userObject, long timestamp )
    {
        prepareDataUpdate( userObject, timestamp );
        
        updateDataImpl( userObject, timestamp );
        
        onDataUpdated( userObject, timestamp, false );
    }
    
    void onViewportChanged( int viewportX, int viewportY, int viewportWidth, int viewportHeight )
    {
        if ( updateListeners != null )
        {
            for ( int i = 0; i < updateListeners.length; i++ )
                updateListeners[i].onViewportChanged( gameData, viewportX, viewportY, viewportWidth, viewportHeight );
        }
    }
    
    /**
     * Gets, whether the last update of these data has been done while in realtime mode.
     * @return whether the last update of these data has been done while in realtime mode.
     */
    public final boolean isUpdatedInRealtimeMode()
    {
        return ( updatedInRealtimeMode );
    }
    
    /**
     * camera position in meters
     * 
     * @param position output buffer
     */
    public abstract void getCameraPosition( TelemVect3 position );
    
    /**
     * Gets camera position in meters.
     * 
     * @return camera position in meters.
     */
    public abstract float getCameraPositionX();
    
    /**
     * Gets camera position in meters.
     * 
     * @return camera position in meters.
     */
    public abstract float getCameraPositionY();
    
    /**
     * Gets camera position in meters.
     * 
     * @return camera position in meters.
     */
    public abstract float getCameraPositionZ();
    
    /**
     * camera orientation
     * 
     * @param orientation output buffer
     */
    public abstract void getCameraOrientation( TelemVect3 orientation );
    
    /**
     * Gets the current ambient color.
     * 
     * @return the current ambient color.
     */
    public abstract java.awt.Color getAmbientColor();
    
    private final TelemVect3 camPos = new TelemVect3();
    private final TelemVect3 carPos = new TelemVect3();
    
    /**
     * Gets the vehicle closest to camera.
     * 
     * @return the vehicle or <code>null</code>, if N/A.
     */
    public final VehicleScoringInfo getVehicleScoringInfoClosestToCamera()
    {
        //if ( !isUpdatedInRealtimeMode() )
        //    return ( null );
        
        VehicleScoringInfo viewedVSI = null;
        
        getCameraPosition( camPos );
        camPos.invert();
        
        float closestDist = Float.MAX_VALUE;
        
        final ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        int n = scoringInfo.getNumVehicles();
        
        for ( short i = 0; i < n; i++ )
        {
            scoringInfo.getVehicleScoringInfo( i ).getWorldPosition( carPos );
            
            float dist = carPos.getDistanceToSquared( camPos );
            
            if ( dist < closestDist )
            {
                closestDist = dist;
                viewedVSI = scoringInfo.getVehicleScoringInfo( i );
            }
        }
        
        return ( viewedVSI );
    }
    
    protected GraphicsInfo( LiveGameData gameData )
    {
        this.gameData = gameData;
    }
}
