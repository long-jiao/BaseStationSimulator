
package org.dslforum.cwmp_1_0;

/** 
 * Schema fragment(s) for this class:
 * <pre>
 * &lt;xs:complexType xmlns:ns="urn:dslforum-org:cwmp-1-0" xmlns:ns1="http://schemas.xmlsoap.org/soap/encoding/" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="OptionList">
 *   &lt;xs:complexContent>
 *     &lt;xs:restriction base="ns1:Array">
 *       &lt;xs:sequence>
 *         &lt;xs:element type="ns:OptionStruct" name="OptionStruct" minOccurs="0" maxOccurs="unbounded"/>
 *       &lt;/xs:sequence>
 *       &lt;xs:attribute use="required" ref="ns1:arrayType"/>
 *     &lt;/xs:restriction>
 *   &lt;/xs:complexContent>
 * &lt;/xs:complexType>
 * </pre>
 */
public class OptionList extends Array1
{
}
