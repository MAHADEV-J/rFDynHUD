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
package net.ctdp.rfdynhud.editor.properties;

import net.ctdp.rfdynhud.editor.RFDynHUDEditor;
import net.ctdp.rfdynhud.editor.hiergrid.GridItemsHandler;
import net.ctdp.rfdynhud.editor.hiergrid.HierarchicalTable;
import net.ctdp.rfdynhud.editor.hiergrid.HierarchicalTableModel;
import net.ctdp.rfdynhud.properties.PosSizeProperty;
import net.ctdp.rfdynhud.properties.Property;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;

/**
 * Insert class comment here.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class PropertiesEditorTableModel extends HierarchicalTableModel<Property>
{
    private static final long serialVersionUID = -5111097521627270775L;
    
    public static final GridItemsHandler<Property> ITEMS_HANDLER = new GridItemsHandler<Property>();
    
    private final RFDynHUDEditor editor;
    private final PropertiesEditor propsEditor;
    
    public final RFDynHUDEditor getRFDynHUDEditor()
    {
        return ( editor );
    }
    
    public final PropertiesEditor getPropertiesEditor()
    {
        return ( propsEditor );
    }
    
    @Override
    protected void setValueImpl( HierarchicalTable<Property> table, Property property, int index, Object newValue )
    {
        if ( editor != null )
        {
            if ( WidgetPropertyChangeListener.needsAreaClear( property ) )
                editor.getEditorPanel().clearSelectedWidgetRegion();
        }
        
        Object oldValue = getValueImpl( table, property, index );
        property.setValue( newValue );
        propsEditor.invokeChangeListeners( property, oldValue, newValue, table.getSelectedRow(), table.getSelectedColumn() );
        
        if ( ( editor != null ) && ( property.getKeeper() != null ) )
        {
            editor.onWidgetChanged( ( property.getKeeper() instanceof Widget ) ? (Widget)property.getKeeper() : null, property.getName(), property instanceof PosSizeProperty );
            editor.getEditorPanel().setSelectedWidget( editor.getEditorPanel().getSelectedWidget(), false ); // refresh properties editor in case the propertis toggles the display of other properties
        }
    }
    
    @Override
    protected Object getValueImpl( HierarchicalTable<Property> table, Property property, int index )
    {
        if ( index == 0 )
            return ( property.getNameForDisplay() );
        
        return ( property.getValueForEditor() );
    }
    
    public PropertiesEditorTableModel( RFDynHUDEditor editor, PropertiesEditor propsEditor )
    {
        super( ITEMS_HANDLER, propsEditor.getPropertiesList(), 2 );
        
        this.editor = editor;
        this.propsEditor = propsEditor;
    }
    
    public static HierarchicalTable<Property> newTable( RFDynHUDEditor editor, PropertiesEditor propsEditor )
    {
        HierarchicalTable<Property> table = new HierarchicalTable<Property>( new PropertiesEditorTableModel( editor, propsEditor ), new TableCellRendererProviderImpl() );
        table.setTableHeader( null );
        
        return ( table );
    }
}
