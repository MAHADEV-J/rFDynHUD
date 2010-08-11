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
package net.ctdp.rfdynhud.properties;

import net.ctdp.rfdynhud.values.Position;
import net.ctdp.rfdynhud.values.Size;
import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;

/**
 * Don't use this at home!
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class __PropsPrivilegedAccess
{
    public static final void addPosition( WidgetToPropertyForwarder w2pf, Position position )
    {
        w2pf.addPosition( position );
    }
    
    public static final void addSize( WidgetToPropertyForwarder w2pf, Size size )
    {
        w2pf.addSize( size );
    }
    
    public static final BooleanProperty newBooleanProperty( WidgetsConfiguration widgetsConfig, String propertyName, String nameForDisplay, boolean defaultValue, boolean readonly )
    {
        return ( new BooleanProperty( widgetsConfig, propertyName, nameForDisplay, defaultValue, readonly ) );
    }
    
    public static final boolean isWidgetsConfigProperty( Property property )
    {
        return ( property.widgetsConfig != null );
    }
    
    public static final void setCellRenderer( Object renderer, Property property )
    {
        property.cellRenderer = renderer;
    }
    
    public static final Object getCellRenderer( Property property )
    {
        return ( property.cellRenderer );
    }
    
    public static final void setCellEditor( Object editor, Property property )
    {
        property.cellEditor = editor;
    }
    
    public static final Object getCellEditor( Property property )
    {
        return ( property.cellEditor );
    }
}
