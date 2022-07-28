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
package net.ctdp.rfdynhud.editor.presets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.ctdp.rfdynhud.editor.RFDynHUDEditor;
import net.ctdp.rfdynhud.editor.__EDPrivilegedAccess;
import net.ctdp.rfdynhud.editor.hiergrid.GridItemsContainer;
import net.ctdp.rfdynhud.editor.hiergrid.HierarchicalTable;
import net.ctdp.rfdynhud.editor.properties.DefaultPropertiesContainer;
import net.ctdp.rfdynhud.editor.properties.PropertiesEditor;
import net.ctdp.rfdynhud.editor.properties.PropertiesEditorTableModel;
import net.ctdp.rfdynhud.editor.properties.PropertyChangeListener;
import net.ctdp.rfdynhud.gamedata.__GDPrivilegedAccess;
import net.ctdp.rfdynhud.properties.AbstractPropertiesKeeper;
import net.ctdp.rfdynhud.properties.EnumProperty;
import net.ctdp.rfdynhud.properties.Property;

public class EditorPresetsWindow extends JDialog implements PropertyChangeListener
{
    private static final long serialVersionUID = 2722252740556901190L;
    
    private final RFDynHUDEditor editor;
    
    private final PropertiesEditor propertiesEditor = new PropertiesEditor();
    private HierarchicalTable<Property> editorTable = null;
    
    private final Map<String, ChangedProperty> changed = new HashMap<String, ChangedProperty>();
    private int maxChangeId = 0;
    
    private JCheckBox cbxAutoApply;
    private JButton btnOk, btnApply, btnClose;
    
    private boolean setWindowSize = true;
    
    private final EnumProperty<ScaleType> defaultScaleType = new EnumProperty<ScaleType>( "defaultScaleType", ScaleType.PERCENTS );
    
    public void setDontSetWindowSize()
    {
        setWindowSize = false;
    }
    
    public void setDefaultScaleType( ScaleType scaleType )
    {
        this.defaultScaleType.setEnumValue( scaleType );
    }
    
    public final ScaleType getDefaultScaleType()
    {
        return ( defaultScaleType.getEnumValue() );
    }
    
    public void setAutoApply( boolean autoApply )
    {
        cbxAutoApply.setSelected( autoApply );
    }
    
    public final boolean getAutoApply()
    {
        return ( cbxAutoApply.isSelected() );
    }
    
    private void setDirtyFlag()
    {
        btnOk.setEnabled( true );
        btnApply.setEnabled( true );
    }
    
    private void resetDirtyFlag()
    {
        changed.clear();
        
        btnOk.setEnabled( false );
        btnApply.setEnabled( false );
    }
    
    private final boolean getDirtyFlag()
    {
        return ( !changed.isEmpty() );
    }
    
    private void fillPropertiesList( GridItemsContainer<Property> props )
    {
        DefaultPropertiesContainer propsCont = new DefaultPropertiesContainer( props );
        
        propsCont.addGroup( "General" );
        
        propsCont.addProperty( defaultScaleType );
        
        __EDPrivilegedAccess.getEditorPresetsProperties( editor.getEditorPresets(), propsCont );
    }
    
    private Component createPropertiesEditor()
    {
        fillPropertiesList( propertiesEditor.getPropertiesList() );
        
        propertiesEditor.addChangeListener( this );
        
        editorTable = PropertiesEditorTableModel.newTable( editor, propertiesEditor );
        
        JScrollPane sp = editorTable.createScrollPane();
        sp.setPreferredSize( new Dimension( 300, 500 ) );
        
        return ( sp );
    }
    
    @Override
    public void onPropertyChanged( Property property, Object oldValue, Object newValue, int row, int column )
    {
        if ( !changed.containsKey( property.getName() ) )
        {
            changed.put( property.getName(), new ChangedProperty( property, oldValue, ++maxChangeId ) );
        }
        else
        {
            changed.get( property.getName() ).setChangeId( ++maxChangeId );
        }
        
        setDirtyFlag();
        
        if ( cbxAutoApply.isSelected() )
        {
            apply();
        }
    }
    
    private ChangedProperty[] getSortedChangedProperties()
    {
        ChangedProperty[] array = new ChangedProperty[ changed.size() ];
        
        int i = 0;
        for ( ChangedProperty cp : changed.values() )
        {
            array[i++] = cp;
        }
        
        Arrays.sort( array );
        
        return ( array );
    }
    
    private void apply()
    {
        __GDPrivilegedAccess.applyEditorPresets( editor.getEditorPresets(), editor.getGameData() );
        editor.getWidgetsConfiguration().setAllDirtyFlags();
        editor.repaintEditorPanel();
        
        resetDirtyFlag();
    }
    
    private void requestClose()
    {
        if ( getDirtyFlag() )
        {
            int result = JOptionPane.showConfirmDialog( this, "There are unapplied property changes. Do you want to apply them now?", "Unapplied changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
            
            if ( result == JOptionPane.CANCEL_OPTION )
                return;
            
            if ( result == JOptionPane.YES_OPTION )
            {
                apply();
            }
            
            if ( result == JOptionPane.NO_OPTION )
            {
                for ( ChangedProperty cp : getSortedChangedProperties() )
                {
                    cp.resetValue();
                }
            }
            
            resetDirtyFlag();
        }
        
        setVisible( false );
    }
    
    public EditorPresetsWindow( RFDynHUDEditor editor )
    {
        super( editor.getMainWindow(), "rfDynHUD Editor Presets", false );
        
        AbstractPropertiesKeeper.setKeeper( defaultScaleType, null );
        
        this.editor = editor;
        setLocationRelativeTo( editor.getMainWindow() );
        
        JPanel cp = (JPanel)this.getContentPane();
        
        cp.setLayout( new BorderLayout( 5, 5 ) );
        
        cp.add( createPropertiesEditor(), BorderLayout.CENTER );
        
        JPanel buttons0 = new JPanel( new BorderLayout() );
        JPanel buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        btnOk = new JButton( "Ok" );
        btnOk.setToolTipText( "Did you know? Ok means Apply + Close. ;)" );
        btnOk.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                apply();
                requestClose();
            }
        } );
        btnOk.setEnabled( false );
        buttons.add( btnOk );
        
        btnApply = new JButton( "Apply" );
        btnApply.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                apply();
            }
        } );
        btnApply.setEnabled( false );
        buttons.add( btnApply );
        
        btnClose = new JButton( "Close" );
        btnClose.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                requestClose();
            }
        } );
        buttons.add( btnClose );
        
        buttons0.add( buttons, BorderLayout.CENTER );
        
        cbxAutoApply = new JCheckBox( "Auto apply" );
        cbxAutoApply.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                if ( ( (JCheckBox)e.getSource() ).isSelected() && getDirtyFlag() )
                {
                    apply();
                }
            }
        } );
        buttons0.add( cbxAutoApply, BorderLayout.WEST );
        
        this.add( buttons0, BorderLayout.SOUTH );
        
        this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        
        addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowOpened( WindowEvent e )
            {
                editorTable.applyToModel();
                
                if ( !setWindowSize )
                    return;
                
                pack();
                
                setWindowSize = false;
            }
            
            @Override
            public void windowClosing( WindowEvent e )
            {
                requestClose();
            }
        } );
    }
}
