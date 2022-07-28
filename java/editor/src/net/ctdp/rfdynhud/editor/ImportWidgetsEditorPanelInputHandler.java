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
package net.ctdp.rfdynhud.editor;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JOptionPane;

import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class ImportWidgetsEditorPanelInputHandler implements MouseListener, KeyListener
{
    private final RFDynHUDEditor editor;
    private final WidgetsEditorPanel editorPanel;
    
    private WidgetsConfiguration widgetsConfig;
    
    private void importWidget()
    {
        if ( editorPanel.getSelectedWidget() != null )
        {
            Boolean result = new WidgetImportManager( editor, editorPanel ).importWidget( editorPanel.getSelectedWidget() );
            if ( result != null )
            {
                if ( result )
                {
                    JOptionPane.showMessageDialog( editorPanel, "Import successful.", "Import", JOptionPane.INFORMATION_MESSAGE );
                    
                    //JDialog dialog = (JDialog)editorPanel.getRootPane().getParent();
                    //dialog.dispose();
                }
                else
                {
                    JOptionPane.showMessageDialog( editorPanel, "Import failed.", "Import", JOptionPane.ERROR_MESSAGE );
                }
            }
        }
    }
    
    private Widget getWidgetUnderMouse( int x, int y )
    {
        for ( int i = widgetsConfig.getNumWidgets() - 1; i >= 0; i-- )
        {
            Widget widget = widgetsConfig.getWidget( i );
            
            int wx = widget.getPosition().getEffectiveX();
            int wy = widget.getPosition().getEffectiveY();
            int ww = widget.getSize().getEffectiveWidth();
            int wh = widget.getSize().getEffectiveHeight();
            
            if ( ( wx <= x ) && ( wx + ww > x ) && ( wy <= y ) && ( wy + wh > y ) )
            {
                return ( widget );
            }
        }
        
        return ( null );
    }
    
    @Override
    public void mousePressed( MouseEvent e )
    {
        // I have no idea, why this is necessary.
        editorPanel.requestFocus();
        
        int x = Math.round( e.getX() * editorPanel.getRecipScaleFactor() );
        int y = Math.round( e.getY() * editorPanel.getRecipScaleFactor() );
        
        editorPanel.setSelectedWidget( getWidgetUnderMouse( x, y ), false );
    }
    
    @Override
    public void mouseReleased( MouseEvent e )
    {
    }
    
    @Override
    public void mouseClicked( MouseEvent e )
    {
        if ( ( e.getButton() == MouseEvent.BUTTON1 ) && ( editorPanel.getSelectedWidget() != null ) )
        {
            boolean doubleClick = e.getClickCount() == 2;
            
            editorPanel.setSelectedWidget( editorPanel.getSelectedWidget(), doubleClick );
            
            if ( doubleClick )
                importWidget();
        }
    }
    
    @Override
    public void mouseEntered( MouseEvent e )
    {
    }
    
    @Override
    public void mouseExited( MouseEvent e )
    {
    }
    
    @Override
    public void keyPressed( KeyEvent e )
    {
        switch ( e.getKeyCode() )
        {
            case KeyEvent.VK_ENTER:
                importWidget();
                break;
            case KeyEvent.VK_ESCAPE:
                ( (Window)editorPanel.getRootPane().getParent() ).dispose();
                break;
        }
    }
    
    @Override
    public void keyReleased( KeyEvent e )
    {
    }
    
    @Override
    public void keyTyped( KeyEvent e )
    {
    }
    
    public ImportWidgetsEditorPanelInputHandler( RFDynHUDEditor editor, WidgetsEditorPanel editorPanel, WidgetsConfiguration widgetsConfig )
    {
        this.editor = editor;
        this.editorPanel = editorPanel;
        this.widgetsConfig = widgetsConfig;
    }
}
