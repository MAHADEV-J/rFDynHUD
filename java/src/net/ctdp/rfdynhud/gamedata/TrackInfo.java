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
 * Model of the currently used track
 * 
 * @author Marvin Froehlich
 */
public abstract class TrackInfo
{
    /**
     * Checks, whether this {@link TrackInfo} has successfully been read from somewhere.
     * 
     * @return whether this {@link TrackInfo} has successfully been read from somewhere.
     */
    public abstract boolean isValid();
    
    protected void reset()
    {
    }
    
    protected abstract void updateImpl();
    
    /**
     * WARNING:<br>
     * This operation may take a long time.
     */
    final void update()
    {
        updateImpl();
    }
    
    /**
     * Gets the track's name.
     * 
     * @return the track's name.
     */
    public abstract String getTrackName();
    
    /**
     * Gets last read track race laps.
     * 
     * @return last read track race laps.
     */
    public abstract int getRaceLaps();
    
    /**
     * <p>
     * Gets the track abstraction (waypoints) of the current track.
     * </p>
     * WARNING:<br>
     * This operation may take a long time.
     * 
     * @return the track abstraction (waypoints) of the current track.
     */
    public abstract Track getTrack();
    
    /**
     * Create a new instance.
     * 
     * @param profileInfo
     */
    protected TrackInfo( ProfileInfo profileInfo )
    {
    }
}
