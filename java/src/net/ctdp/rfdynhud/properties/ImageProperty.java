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
package net.ctdp.rfdynhud.properties;

import net.ctdp.rfdynhud.render.ImageTemplate;
import net.ctdp.rfdynhud.util.TextureManager;

/**
 * The {@link ImageProperty} serves for customizing an image.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class ImageProperty extends StringProperty
{
    public static final boolean DEFAULT_NO_IMAGE_ALOWED = false;
    
    private final String defaultValue;
    
    private final boolean noImageAllowed;
    
    /**
     * Gets whether "no image" is allowed for this property.
     * 
     * @return whether "no image" is allowed for this property.
     */
    public final boolean getNoImageAllowed()
    {
        return ( noImageAllowed );
    }
    
    /**
     * Sets the image name.
     * 
     * @param imageName
     */
    public final void setImageName( String imageName )
    {
        setStringValue( imageName );
    }
    
    /**
     * Gets the currently selected image name.
     * 
     * @return the currently selected image name.
     */
    public final String getImageName()
    {
        return ( getStringValue() );
    }
    
    /**
     * Gets whether this property is set to "no image".
     * 
     * @return whether this property is set to "no image".
     */
    public final boolean isNoImage()
    {
        return ( ( getStringValue() == null ) || getStringValue().equals( "" ) );
    }
    
    /**
     * Gets the {@link ImageTemplate} defined by this {@link ImageProperty}.
     * 
     * @return the {@link ImageTemplate} defined by this {@link ImageProperty} or <code>null</code>, if set to no image.
     */
    public final ImageTemplate getImage()
    {
        if ( isNoImage() )
            return ( null );
        
        return ( TextureManager.getImage( getImageName() ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValue( PropertyLoader loader, String value )
    {
        // backwards compatiblity!
        
        if ( loader.getSourceVersion().getBuild() < 91 )
        {
            if ( !value.equals( "" ) && ( getKeeper() != null ) )
            {
                String value2 = value;
                
                if ( getKeeper().getClass().getSimpleName().startsWith( "ETV" ) )
                {
                    if ( value.startsWith( "etv2010/" ) )
                        value2 = "etv2010/telemetry/" + value.substring( 8 );
                }
                else
                {
                    if ( value.startsWith( "default_" ) )
                        value2 = "standard/" + value.substring( 8 );
                    else
                        value2 = "standard/" + value;
                }
                
                if ( value2.equals( defaultValue ) )
                    value = value2;
            }
        }
        
        setValue( value );
    }
    
    /**
     * 
     * @param name the technical name used internally. See {@link #getName()}.
     * @param nameForDisplay the name displayed in the editor. See {@link #getNameForDisplay()}. If <code>null</code> is passed, the value of the name parameter is used.
     * @param defaultValue the default value
     * @param readonly read only property?
     * @param noImageAllowed allow "no image" for this property?
     */
    public ImageProperty( String name, String nameForDisplay, String defaultValue, boolean readonly, boolean noImageAllowed )
    {
        super( name, nameForDisplay, defaultValue, true, readonly, PropertyEditorType.IMAGE );
        
        this.defaultValue = defaultValue;
        
        this.noImageAllowed = noImageAllowed;
    }
    
    /**
     * 
     * @param name the technical name used internally. See {@link #getName()}.
     * @param nameForDisplay the name displayed in the editor. See {@link #getNameForDisplay()}. If <code>null</code> is passed, the value of the name parameter is used.
     * @param defaultValue the default value
     */
    public ImageProperty( String name, String nameForDisplay, String defaultValue )
    {
        this( name, nameForDisplay, defaultValue, false, DEFAULT_NO_IMAGE_ALOWED );
    }
    
    /**
     * 
     * @param name the technical name used internally. See {@link #getName()}. 'nameForDisplay' is set to the same value.
     * @param defaultValue the default value
     * @param readonly read only property?
     */
    public ImageProperty( String name, String defaultValue, boolean readonly )
    {
        this( name, null, defaultValue, readonly, DEFAULT_NO_IMAGE_ALOWED );
    }
    
    /**
     * 
     * @param name the technical name used internally. See {@link #getName()}. 'nameForDisplay' is set to the same value.
     * @param defaultValue the default value
     */
    public ImageProperty( String name, String defaultValue )
    {
        this( name, defaultValue, false );
    }
}
