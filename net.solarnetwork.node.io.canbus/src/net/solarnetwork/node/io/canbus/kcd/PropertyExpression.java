//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.04.06 at 08:03:13 AM NZST 
//


package net.solarnetwork.node.io.canbus.kcd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * A datum property derived from an expression.
 * 
 * <p>Java class for PropertyExpression complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PropertyExpression">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute ref="{urn:solarnetwork:datum:1.0}datum-property use="required""/>
 *       &lt;attribute ref="{urn:solarnetwork:datum:1.0}datum-property-classification default="i""/>
 *       &lt;attribute ref="{urn:solarnetwork:datum:1.0}expression-lang default="net.solarnetwork.common.expr.spel.SpelExpressionService""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertyExpression", namespace = "urn:solarnetwork:datum:1.0", propOrder = {
    "value"
})
public class PropertyExpression {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "datum-property", namespace = "urn:solarnetwork:datum:1.0", required = true)
    protected String datumProperty;
    @XmlAttribute(name = "datum-property-classification", namespace = "urn:solarnetwork:datum:1.0")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String datumPropertyClassification;
    @XmlAttribute(name = "expression-lang", namespace = "urn:solarnetwork:datum:1.0")
    protected String expressionLang;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * The datum property name to populate for this
     *                             expression.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatumProperty() {
        return datumProperty;
    }

    /**
     * Sets the value of the datumProperty property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatumProperty(String value) {
        this.datumProperty = value;
    }

    /**
     * The datum property classification of the property to
     *                             populate for this expression.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatumPropertyClassification() {
        if (datumPropertyClassification == null) {
            return "i";
        } else {
            return datumPropertyClassification;
        }
    }

    /**
     * Sets the value of the datumPropertyClassification property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatumPropertyClassification(String value) {
        this.datumPropertyClassification = value;
    }

    /**
     * The datum property classification of the property to
     *                             populate for this expression.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExpressionLang() {
        if (expressionLang == null) {
            return "net.solarnetwork.common.expr.spel.SpelExpressionService";
        } else {
            return expressionLang;
        }
    }

    /**
     * Sets the value of the expressionLang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExpressionLang(String value) {
        this.expressionLang = value;
    }

}