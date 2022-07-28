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
package net.ctdp.rfdynhud.editor.hiergrid;

import java.awt.Color;
import java.awt.Graphics;

/**
 * @param <P> the property type
 * 
 * @author Marvin Froehlich
 */
public class KeyCellRenderer<P extends Object> extends KeyValueCellRenderer<P, KeyRenderLabel>
{
    private static final long serialVersionUID = 663331747917701155L;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintBorder( KeyRenderLabel c, Graphics g, int x, int y, int width, int height, int row, int column, Color borderColor )
    {
        if ( row > 0 )
        {
            HierarchicalTable<?> table = (HierarchicalTable<?>)c.getParent().getParent();
            
            int offsetX = 0;
            
            boolean isDataRow = table.getModel().isDataRow( row - 1 );
            if ( ( isDataRow && table.getStyle().getIndentKeyBorders() ) || ( !isDataRow && table.getStyle().getIndentHeaders() ) )
            {
                int level = table.getModel().getLevel( row - 1 );
                int indent = table.getStyle().getLevelIndentation();
                offsetX = level * indent;
            }
            
            super.paintBorder( c, g, offsetX + x, y, width - offsetX, height, row, column, borderColor );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareComponent( KeyRenderLabel component, HierarchicalTable<P> table, P property, Object value, boolean isSelected, boolean hasFocus, int row, int column, boolean forEditor )
    {
        super.prepareComponent( component, table, property, value, isSelected, hasFocus, row, column, forEditor );
        
        HierarchicalGridStyle style = table.getStyle();
        HierarchicalTableModel<P> tm = table.getModel();
        
        component.setLevel( tm.getLevel( row ), style.getLevelIndentation() );
        component.setLastInGroup( tm.getLastInGroup( row ) );
        
        component.setForeground( style.getKeyCellFontColor() );
        //if ( !component.getFont().isBold() )
        //    component.setFont( component.getFont().deriveFont( component.getFont().getStyle() | java.awt.Font.BOLD ) );
        component.setFont( style.getKeyCellFont() );
        
        component.setText( String.valueOf( value ) );
    }
    
    @Override
    public final Object getCellEditorValue()
    {
        return ( null );
    }
    
    public KeyCellRenderer()
    {
        super( true, new KeyRenderLabel() );
    }
}
