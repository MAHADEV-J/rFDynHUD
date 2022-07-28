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
import net.ctdp.rfdynhud.gamedata.DrivingAids;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.render.ImageTemplate;

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
class _rf1_DrivingAids extends DrivingAids
{
    private static final int NUM_AIDS = 0;
    
    @Override
    protected void updateDataImpl( Object userObject, long timestamp )
    {
    }
    
    @Override
    public void readFromStream( InputStream in, EditorPresets editorPresets ) throws IOException
    {
        final long now = System.nanoTime();
        
        //readFromStreamImpl( in );
        
        prepareDataUpdate( editorPresets, now );
        
        if ( editorPresets != null )
        {
            applyEditorPresets( editorPresets );
        }
        
        onDataUpdated( editorPresets, now );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadDefaultValues( EditorPresets editorPresets )
    {
        /*
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
        */
    }
    
    @Override
    public void writeToStream( OutputStream out ) throws IOException
    {
        //out.write( buffer, 0, BUFFER_SIZE );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNumAids()
    {
        return ( NUM_AIDS );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getAidIndexTractionControl()
    {
        return ( -1 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getAidIndexTractionAntiLockBrakes()
    {
        return ( -1 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getAidIndexAutoShift()
    {
        return ( -1 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getAidIndexInvulnerability()
    {
        return ( -1 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final String getAidName( int index )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getAidState( int index )
    {
        return ( -1 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getAidStateName( int index, int state )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getMinState( int index )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getMaxState( int index )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNumStates( int index )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ImageTemplate getAidIcon( int index, int state )
    {
        throw new IllegalArgumentException( "There is no aid with the index " + index + ". Only " + getNumAids() + " available." );
    }
    
    _rf1_DrivingAids( LiveGameData gameData )
    {
        super( gameData );
    }
}
