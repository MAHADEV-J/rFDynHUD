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
package net.ctdp.rfdynhud.valuemanagers;

/**
 * An implementation specific managed value.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public interface ManagedValue
{
    /**
     * Initializes/resets the {@link ManagedValue}.
     * 
     * @param nanoTime the starting time stamp
     */
    public void init( long nanoTime );
    
    /**
     * Invokes the implementation specific update code.
     * 
     * @param nanoTime the current timestamp in nano seconds
     * @param frameCounter the current frame index
     * @param force force clock to <code>true</code>.
     */
    public void update( long nanoTime, long frameCounter, boolean force );
}
