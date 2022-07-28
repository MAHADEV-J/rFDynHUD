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
package net.ctdp.rfdynhud.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.ctdp.rfdynhud.render.ImageTemplate;

/**
 * ImageIO image loading is pretty slow. This is a simple but fast texture loading implementation.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class TextureManager
{
    private static ImageTemplate MISSING_IMAGE = null;
    
    public static BufferedImage createMissingImage( int width, int height )
    {
        BufferedImage bi = new BufferedImage( width, height, BufferedImage.TYPE_3BYTE_BGR );
        
        Graphics2D g = bi.createGraphics();
        
        final int sx = 5;
        final int sy = 5;
        final int sx2 = sx + sx;
        final int sy2 = sy + sy;
        
        for ( int y = 0; y < height; y += sy )
        {
            for ( int x = 0; x < width; x += 5 )
            {
                if ( ( y % sy2 ) == 0 )
                {
                    if ( ( x % sx2 ) == 0 )
                        g.setColor( Color.LIGHT_GRAY );
                    else
                        g.setColor( Color.WHITE );
                }
                else
                {
                    if ( ( x % sx2 ) == 0 )
                        g.setColor( Color.WHITE );
                    else
                        g.setColor( Color.LIGHT_GRAY );
                }
                
                g.fillRect( x, y, 5, 5 );
            }
        }
        
        return ( bi );
    }
    
    public static final ImageTemplate getMissingImage()
    {
        if ( MISSING_IMAGE == null )
        {
            MISSING_IMAGE = new ImageTemplate( "MISSING", createMissingImage( 128, 128 ) );
        }
        
        return ( MISSING_IMAGE );
    }
    
    private static final ImageCache cache = new DefaultImageCache();
    
    public static ImageTemplate getImage( String name, boolean useCache )
    {
        if ( File.separatorChar != '/' )
            name = name.replace( '/', File.separatorChar );
        if ( File.separatorChar != '\\' )
            name = name.replace( '\\', File.separatorChar );
        
        File f = new File( name );
        if ( !f.isAbsolute() )
            f = new File( __UtilHelper.imagesFolder, name );
        
        ImageTemplate template = null;
        
        if ( !f.exists() )
        {
            if ( useCache )
            {
                template = cache.get( name );
                
                if ( template != getMissingImage() )
                {
                    RFDHLog.exception( "[ERROR] Unable to read input file \"" + f.getAbsolutePath() + "\"." );
                    //RFDHLog.exception( new Exception() );
                    
                    template = getMissingImage();
                    
                    cache.remove( name );
                    cache.add( name, 0, template );
                }
            }
            else
            {
                RFDHLog.exception( "[ERROR] Unable to read input file \"" + f.getAbsolutePath() + "\"." );
                //RFDHLog.exception( new Exception() );
                
                template = getMissingImage();
            }
            
            return ( template );
        }
        
        cache.check( name, f.lastModified() );
        template = useCache ? cache.get( name ) : null;
        
        //System.out.println( ( ( template != null ) ? "found in cache" : "not found in cache" ) );
        
        if ( template != null )
        {
            if ( template != getMissingImage() )
            {
                return ( template );
            }
            
            if ( useCache )
                cache.remove( name );
            
            template = null;
        }
        
        BufferedImage image = null;
        
        try
        {
            image = ImageIO.read( f );
        }
        catch ( IOException e )
        {
            RFDHLog.exception( "[ERROR] Unable to read input file \"" + f.getAbsolutePath() + "\"." );
            return ( getMissingImage() );
        }
        
        template = new ImageTemplate( name, image );
        
        if ( useCache )
            cache.add( name, f.lastModified(), template );
        
        return ( template );
    }
    
    public static ImageTemplate getImage( String name )
    {
        return ( getImage( name, true ) );
    }
    
    public static void removeImageFromCache( String name )
    {
        if ( File.separatorChar != '/' )
            name = name.replace( '/', File.separatorChar );
        if ( File.separatorChar != '\\' )
            name = name.replace( '\\', File.separatorChar );
        
        cache.remove( name );
        //biCache.remove( name );
    }
}
