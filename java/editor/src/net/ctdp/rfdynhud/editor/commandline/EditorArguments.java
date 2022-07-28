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
package net.ctdp.rfdynhud.editor.commandline;

import java.util.ArrayList;
import java.util.List;

/**
 * Insert class comment here.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class EditorArguments
{
    private final List<String> excludedJars = new ArrayList<String>();
    private String[] additionalJars = null;
    private String objectFactory = null;
    
    void addExcludedJar( String excludedJar )
    {
        excludedJars.add( excludedJar );
    }
    
    public final List<String> getExcludedJars()
    {
        return ( excludedJars );
    }
    
    void setAdditionalJars( String[] jars )
    {
        this.additionalJars = jars;
    }
    
    public final String[] getAdditionalJars()
    {
        return ( additionalJars );
    }
    
    void setObjectFactory( String objectFactory )
    {
        this.objectFactory = objectFactory;
    }
    
    public final String getObjectFactory()
    {
        return ( objectFactory );
    }
}
