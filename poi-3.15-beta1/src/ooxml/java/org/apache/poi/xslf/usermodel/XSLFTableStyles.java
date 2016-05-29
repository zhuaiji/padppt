/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.xslf.usermodel;

import static org.apache.poi.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableStyle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableStyleList;

@Beta
public class XSLFTableStyles extends POIXMLDocumentPart implements Iterable<XSLFTableStyle>{
    private CTTableStyleList _tblStyleLst;
    private List<XSLFTableStyle> _styles;

    public XSLFTableStyles(){
        super();
    }

    /**
     * @since POI 3.14-Beta1
     */
    public XSLFTableStyles(PackagePart part) throws IOException, XmlException {
        super(part);

        _tblStyleLst = CTTableStyleList.Factory.parse(getPackagePart().getInputStream(), DEFAULT_XML_OPTIONS);
        CTTableStyle[] tblStyleArray = _tblStyleLst.getTblStyleArray();
        _styles = new ArrayList<XSLFTableStyle>(tblStyleArray.length);
        for(CTTableStyle c : tblStyleArray){
            _styles.add(new XSLFTableStyle(c));
        }
    }

    /**
     * @deprecated in POI 3.14, scheduled for removal in POI 3.16
     */
    @Deprecated
    public XSLFTableStyles(PackagePart part, PackageRelationship rel) throws IOException, XmlException {
        this(part);
    }
    
    public CTTableStyleList getXmlObject(){
        return _tblStyleLst;
    }

    public Iterator<XSLFTableStyle> iterator(){
        return _styles.iterator();
    }

    public List<XSLFTableStyle> getStyles(){
        return Collections.unmodifiableList(_styles);
    }
}