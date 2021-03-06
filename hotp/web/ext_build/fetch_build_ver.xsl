<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:em="http://www.mozilla.org/2004/em-rdf#"
                xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:r="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions">
    <xsl:output method="text"/>

    <!-- must use the "r:" prefix in XPath, or the JDK6 built-in XSLT won't recognize it -->
    <xsl:template match="/r:RDF/r:Description/em:version/text()"><xsl:copy-of select="."/></xsl:template>

    <xsl:template match="node()">
        <xsl:apply-templates/>
    </xsl:template>
</xsl:stylesheet>
