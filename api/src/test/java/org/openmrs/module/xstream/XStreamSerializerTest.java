/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.xstream;

import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.custommonkey.xmlunit.XMLAssert;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.ConceptName;
import org.openmrs.ConceptSource;
import org.openmrs.OpenmrsObject;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.serialization.xstream.XStreamSerializer;
import org.openmrs.serialization.SerializationException;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Test class that test the features of XStreamSerializer, such as cglib's serialization, build
 * reference for cglib and omit "log", etc
 */
public class XStreamSerializerTest extends BaseModuleContextSensitiveTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
	
	/**
	 * When serialize a proxy of cglib type, we should treat it as a common entity obj without
	 * showing its proxy identity
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldSerializeCGlibCorrectly() throws Exception {
		//Here we use the db data stored in "initialInMemoryTestDataSet.xml" and "standardTestDataset.xml"
		PersonName pn = Context.getPersonService().getPersonNameByUuid("399e3a7b-6482-487d-94ce-c07bb3ca3cc7");
		
		//In Hibernate, it will return a proxy using cglib framework
		assertTrue("The person attribute of PersonName should be a hibernate proxy generated by cglib", HibernateProxy.class
		        .isAssignableFrom(pn.getPerson().getClass()));
		
		String xmlOutput = Context.getSerializationService().serialize(pn, XStreamSerializer.class);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		
		XMLAssert.assertXpathEvaluatesTo("da7f524f-27ce-4bb2-86d6-6d1d05312bd5", "/personName/person/@uuid", xmlOutput);
		XMLAssert.assertXpathEvaluatesTo("2", "/personName/person/personId", xmlOutput);
		XMLAssert.assertXpathEvaluatesTo("M", "/personName/person/gender", xmlOutput);
		XMLAssert.assertXpathEvaluatesTo(sdf.format(pn.getPerson().getBirthdate()), "/personName/person/birthdate",
		    xmlOutput);
		XMLAssert.assertXpathEvaluatesTo("false", "/personName/person/dead", xmlOutput);
		XMLAssert.assertXpathEvaluatesTo("false", "/personName/person/@voided", xmlOutput);
	}
	
	/**
	 * When serializling a cglib proxy, if xstream has already serialized an equal obj with this
	 * proxy, then xstream should build a id reference to that obj instead of inserting a new id.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldBuildReferenceForCGlib() throws Exception {
		//Here we use the db data stored in "initialInMemoryTestDataSet.xml" and "standardTestDataset.xml"
		PersonName pn = Context.getPersonService().getPersonNameByUuid("38a686df-d459-484c-9e7c-3f43a9bced58");
		
		//In Hibernate, it will return a proxy using cglib framework
		assertTrue("The person attribute of PersonName should be a hibernate proxy generated by cglib", HibernateProxy.class
		        .isAssignableFrom(pn.getPerson().getClass()));
		
		// [DJ] THIS IS DEFINITELY WRONG NOW THAT USERS ARE NOT PERSONS [/DJ]
		//creator and person of 'pn' should be the same one, you can find this feature in "initialInMemoryTestDataSet.xml" and "standardTestDataset.xml"
		/*assertEquals(pn.getCreator(), pn.getPerson());
		
		String xmlOutput = Context.getSerializationService().serialize(pn, XStreamSerializer.class);
		XMLAssert.assertXpathValuesEqual("/personName/creator/@id", "/personName/person/@reference", xmlOutput);*/
	}
	
	/**
	 * The "log" attribute shouldn't exist in serialized xml string if it is "private transient".
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldIgnoreLog() throws Exception {
		//Here we use the db data stored in "initialInMemoryTestDataSet.xml" and "standardTestDataset.xml"
		User user = Context.getUserService().getUser(1);
		
		String xmlOutput = Context.getSerializationService().serialize(user, XStreamSerializer.class);
		
		XMLAssert.assertXpathNotExists("/user/log", xmlOutput);
		/*
		 * because we have omited "log" for all classes which are got through "XStreamSerializer.getAllSerializedClasses()",
		 * in the serialized xml string shouldn't contain any element which's name is "log"
		 */
		XMLAssert.assertXpathNotExists("//log", xmlOutput);
	}
	
	
	/**
	 * Regression test #1945 (http://dev.openmrs.org/ticket/1945)
	 * @throws Exception
	 */
	@Test
	public void shouldNotThrowIllegalStateException() throws Exception { 	
		
		ConceptSource conceptSource = new ConceptSource();
		conceptSource.getRetired();
				
		new XStreamSerializer();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldNotBombOnNullListValues() throws Exception {
		List<Object> testList = new ArrayList<Object>();
		testList.add("value1");
		Context.getSerializationService().serialize(testList, XStreamSerializer.class);
		testList.add(null);
		String xml = Context.getSerializationService().serialize(testList, XStreamSerializer.class);
		List<Object> hydratedList = Context.getSerializationService().deserialize(xml, List.class, XStreamSerializer.class);
		Assert.assertEquals(2, hydratedList.size());
		Assert.assertEquals("value1", hydratedList.get(0));
		Assert.assertNull(hydratedList.get(1));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldNotBombOnNullMapValues() throws Exception {
		Map<String, Object> testMap = new LinkedHashMap<String, Object>();
		testMap.put("key1", "value1");
		Context.getSerializationService().serialize(testMap, XStreamSerializer.class);
		testMap.put("key2", null);
		String xml = Context.getSerializationService().serialize(testMap, XStreamSerializer.class);
		Map<String, Object> hydratedMap = Context.getSerializationService().deserialize(xml, Map.class, XStreamSerializer.class);
		Assert.assertEquals(2, hydratedMap.size());
		Iterator<Map.Entry<String, Object>> entries = hydratedMap.entrySet().iterator();
		Map.Entry<String, Object> entry1 = entries.next();
		Assert.assertEquals("key1", entry1.getKey());
		Assert.assertEquals("value1", entry1.getValue());
		Map.Entry<String, Object> entry2 = entries.next();
		Assert.assertEquals("key2", entry2.getKey());
		Assert.assertNull(entry2.getValue());
	}

    /**
     * @see XStreamSerializer#deserialize(String, Class)
     * @verifies not deserialize proxies
     */
    @Test
    public void deserialize_shouldNotDeserializeProxies() throws Exception {
        String serialized = "<dynamic-proxy>" + "<interface>org.openmrs.OpenmrsObject</interface>"
                + "<handler class=\"java.beans.EventHandler\">" + "<target class=\"java.lang.ProcessBuilder\">"
                + "<command>" + "<string>someApp</string>" + "</command></target>" + "<action>start</action>" + "</handler>"
                + "</dynamic-proxy>";

        expectedException.expect(SerializationException.class);
        Context.getSerializationService().deserialize(serialized, OpenmrsObject.class, XStreamSerializer.class);
    }

    /**
     * @see XStreamSerializer#deserialize(String,Class)
     * @verifies ignore entities
     */
    @Test
    public void deserialize_shouldIgnoreEntities() throws Exception {
        String xml = "<!DOCTYPE ZSL [<!ENTITY xxe1 \"some attribute value\" >]>" + "<org.openmrs.ConceptName>"
                + "<name>&xxe1;</name>" + "</org.openmrs.ConceptName>";

        expectedException.expect(SerializationException.class);
        Context.getSerializationService().deserialize(xml, ConceptName.class, XStreamSerializer.class);
    }

}
