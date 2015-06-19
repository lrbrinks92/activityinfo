package org.activityinfo.coverage

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

import javax.xml.parsers.SAXParserFactory


class JacocoParser {
    
    private ProjectCoverage coverage

    JacocoParser(ProjectCoverage coverage) {
        this.coverage = coverage
    }

    private class JacocoHandler extends DefaultHandler {
    
        private String currentPackage
        private FileCoverage currentSourceFile

        void startElement(String ns, String localName, String qName, Attributes atts) {
            switch (qName) {
                case 'package':
                    currentPackage = atts.getValue("name")
                    break
                
                case 'sourcefile':
                    currentSourceFile = coverage.getSource("${currentPackage}/${atts.getValue("name")}")
                    break
                
                case 'line':
                    int lineNumber = Integer.parseInt(atts.getValue('nr'))
                    int coveredInstructions = Integer.parseInt(atts.getValue('ci'))
                    int missedInstructions = Integer.parseInt(atts.getValue('mi'))
                    assert (coveredInstructions+missedInstructions)  > 0
                    currentSourceFile.setCovered(lineNumber, coveredInstructions > 0)
                    break
            }
        }
    }
    
    public void parse(File reportFile) {
        def handler = new JacocoHandler()

        def factory = SAXParserFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);        
        
        def reader = factory.newSAXParser().XMLReader
        reader.setContentHandler(handler)
        reportFile.withInputStream { stream ->
            reader.parse(new InputSource(stream))
        }
    }
}
