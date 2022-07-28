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
package net.ctdp.rfdynhud.editor.input;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class HitTimesEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer
{
    private static final long serialVersionUID = 5993944777559988223L;
    
    private JTextField textField = null;
    
    private Integer oldValue = null;
    
    private void initTextField()
    {
        if ( textField != null )
            return;
        
        textField = new JTextField();
        
        textField.addFocusListener( new FocusAdapter()
        {
            @Override
            public void focusLost( FocusEvent e )
            {
                //stopCellEditing();
            }
        } );
    }
    
    @Override
    public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
    {
        initTextField();
        
        Integer i = (Integer)value;
        
        if ( i == null )
        {
            textField.setText( "1" );
            oldValue = 1;
        }
        else
        {
            textField.setText( i.toString() );
            oldValue = i;
        }
        
        textField.setEnabled( table.getModel().isCellEditable( row, column ) );
        
        return ( textField );
    }
    
    @Override
    public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column )
    {
        initTextField();
        
        Integer i = (Integer)value;
        
        if ( i == null )
        {
            textField.setText( "1" );
            oldValue = 1;
        }
        else
        {
            textField.setText( i.toString() );
            oldValue = i;
        }
        
        return ( textField );
    }
    
    @Override
    public Object getCellEditorValue()
    {
        Integer i = oldValue;
        try
        {
            i = Integer.valueOf( textField.getText() );
        }
        catch ( Throwable t )
        {
            i = oldValue;
        }
        
        if ( i < 1 )
            i = 1;
        else if ( i > 10 )
            i = 10;
        
        oldValue = i;
        
        return ( i );
    }
}
