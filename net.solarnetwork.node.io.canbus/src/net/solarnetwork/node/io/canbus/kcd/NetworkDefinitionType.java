//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.4 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package net.solarnetwork.node.io.canbus.kcd;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Definition of one or more CAN bus networks in one
 *                 file.
 * 
 * <p>Java class for NetworkDefinitionType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="NetworkDefinitionType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Document" type="{http://kayak.2codeornot2code.org/1.0}DocumentType"/>
 *         <element name="Node" type="{http://kayak.2codeornot2code.org/1.0}NodeType" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="Bus" type="{http://kayak.2codeornot2code.org/1.0}BusType" maxOccurs="unbounded"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NetworkDefinitionType", propOrder = {
    "document",
    "node",
    "bus"
})
public class NetworkDefinitionType {

    @XmlElement(name = "Document", required = true)
    protected DocumentType document;
    @XmlElement(name = "Node")
    protected List<NodeType> node;
    @XmlElement(name = "Bus", required = true)
    protected List<BusType> bus;

    /**
     * Gets the value of the document property.
     * 
     * @return
     *     possible object is
     *     {@link DocumentType }
     *     
     */
    public DocumentType getDocument() {
        return document;
    }

    /**
     * Sets the value of the document property.
     * 
     * @param value
     *     allowed object is
     *     {@link DocumentType }
     *     
     */
    public void setDocument(DocumentType value) {
        this.document = value;
    }

    /**
     * Gets the value of the node property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the node property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getNode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NodeType }
     * </p>
     * 
     * 
     * @return
     *     The value of the node property.
     */
    public List<NodeType> getNode() {
        if (node == null) {
            node = new ArrayList<>();
        }
        return this.node;
    }

    /**
     * Gets the value of the bus property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bus property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getBus().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BusType }
     * </p>
     * 
     * 
     * @return
     *     The value of the bus property.
     */
    public List<BusType> getBus() {
        if (bus == null) {
            bus = new ArrayList<>();
        }
        return this.bus;
    }

}
