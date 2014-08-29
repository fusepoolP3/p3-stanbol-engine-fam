package eu.fusepool.enhancer.engines.fise2fam;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.junit.Assert.*;
import static eu.fusepool.enhancer.engines.fise2fam.Constants.*;
import info.dmtree.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.monitor.MonitorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.util.URIref;

import eu.fusepool.enhancer.engines.fise2fam.Fise2FamEngine.SelectorTypes;
import eu.fusepool.p3.vocab.FAM;

public class Fise2FamEngineTest {

	private static final Language CONTENT_LANGUAGE = new Language("en");

	private static final Logger log = LoggerFactory.getLogger(Fise2FamEngineTest.class);
	
    public static final String CONTENT = "The Stanbol enhancer can detect "
    		+ "famous cities such as Paris and people such as Bob Marley.";
    private static final String TEST_ENHANCEMENTS = "enhancements.ttl";

    
    private static final JenaParserProvider rdfParser = new JenaParserProvider();
    private static final JenaSerializerProvider rdfSerializer = new JenaSerializerProvider();
	private static final int NUM_ENTITY_MENTIONS = 2;
	private static final int NUM_ENTITY_ANNOTATION = 4;
	private static final int NUM_TOPIC_CLASSIFICATION = 1;
	private static final int NUM_TOPIC_ANNOTATION = 3;
    private static MGraph origEnhancements;
    private static UriRef ciUri;
    
    
    private final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private final LiteralFactory lf = LiteralFactory.getInstance();
	private ContentItem contentItem;

	private Fise2FamEngine engine;

	private MockComponentContext ctx;

	@BeforeClass
	public static void initTest() throws Exception {
		//read the fise:Enhancements use by the test and validate that they do
		//confirm to the FISE enhancement structure
        InputStream in = Fise2FamEngineTest.class.getClassLoader().getResourceAsStream(TEST_ENHANCEMENTS);
        Assert.assertNotNull("Unable to load reaource '"+TEST_ENHANCEMENTS+"' via Classpath",in);
        origEnhancements = new IndexedMGraph();
        rdfParser.parse(origEnhancements, in, SupportedFormat.TURTLE, null);
        Assert.assertFalse(origEnhancements.isEmpty());
        //parse the ID of the ContentItem form the enhancements
        Iterator<Triple> it = origEnhancements.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
        Assert.assertTrue(it.hasNext());
        Resource id = it.next().getObject();
        Assert.assertTrue(id instanceof UriRef);
        ciUri = (UriRef)id;
        //validate that the enhancements in the file are valid
        EnhancementStructureHelper.validateAllTextAnnotations(
            origEnhancements, CONTENT, null,
            false); //those do not yet contain fise:selection-prefix/suffix values
        
        //create the engine we will use durign the tests
	}
	
	@Before
	public void initContentItem() throws IOException{
		engine = new Fise2FamEngine();
        contentItem = ciFactory.createContentItem(ciUri, 
                new StringSource(CONTENT), new IndexedMGraph(origEnhancements));
	}

	protected void activate(SelectorTypes st, Boolean metadata) throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, "test-engine");
        if(st != null){
        	config.put(Fise2FamEngine.PROPERTY_SELECTOR_TYPE, st.name());
        }
        if(metadata != null){
        	config.put(Fise2FamEngine.PROPERTY_TRANSFORM_METADATA, metadata);
        }
        ctx = new MockComponentContext(config);
        engine.activate(ctx);

	}
	
	@After
	public void deactivate(){
		contentItem = null;
		engine.deactivate(ctx);
		engine = null;
	}
	
	
	@AfterClass
	public static void cleanup(){
		//Nothing to cleanup
	}
	

	@Test
	public void testDefaultEngineConfig() throws EngineException, ConfigurationException{
		activate(null, null);
		Assert.assertNotEquals(EnhancementEngine.CANNOT_ENHANCE, engine.canEnhance(contentItem));
		engine.computeEnhancements(contentItem);
		//log the transformed RDF
		logRdf("Transformed RDF: \n",contentItem.getMetadata());
		//assert EntityMentions
		assertTransformationResults();
	}

	@Test
	public void testOaSelectors() throws EngineException, ConfigurationException{
		activate(SelectorTypes.OA, null);
		Assert.assertNotEquals(EnhancementEngine.CANNOT_ENHANCE, engine.canEnhance(contentItem));
		engine.computeEnhancements(contentItem);
		//log the transformed RDF
		logRdf("Transformed RDF: \n",contentItem.getMetadata());
		//assert EntityMentions
		assertTransformationResults();
	}

	@Test
	public void testBothSelectors() throws EngineException, ConfigurationException{
		activate(SelectorTypes.BOTH, null);
		Assert.assertNotEquals(EnhancementEngine.CANNOT_ENHANCE, engine.canEnhance(contentItem));
		engine.computeEnhancements(contentItem);
		//log the transformed RDF
		logRdf("Transformed RDF: \n",contentItem.getMetadata());
		//assert EntityMentions
		assertTransformationResults();
	}

	@Test
	public void testNoMetadata() throws EngineException, ConfigurationException{
		activate(null, false);
		Assert.assertNotEquals(EnhancementEngine.CANNOT_ENHANCE, engine.canEnhance(contentItem));
		engine.computeEnhancements(contentItem);
		//log the transformed RDF
		logRdf("Transformed RDF: \n",contentItem.getMetadata());
		//assert EntityMentions
		assertTransformationResults();
	}
	
	private void assertTransformationResults() {
		TripleCollection graph = contentItem.getMetadata();
		Iterator<Triple> it = graph.filter(null, RDF_TYPE, FAM.LanguageAnnotation);
		assertTrue("The Language Annotation is missing", it.hasNext());
		NonLiteral langAnnoBody = it.next().getSubject();
		assertFalse("Only a single Language Annotation is expected" ,it.hasNext());
		assertAnnotation(graph, langAnnoBody);
		assertNoValue(graph, langAnnoBody, FAM.selector);
		Literal language = assertSingleValue(graph, langAnnoBody, DC_LANGUAGE, Literal.class);
		assertEquals("en", language.getLexicalForm());
		
		int num = 0;
		it = graph.filter(null, RDF_TYPE, FAM.EntityMention);
		while(it.hasNext()){
			NonLiteral body = it.next().getSubject();
			assertAnnotation(graph, body);
			//assert the entity-type
			UriRef type = assertSingleValue(graph, body, FAM.entity_type, UriRef.class);
			assertTrue(type.equals(OntologicalClasses.DBPEDIA_PLACE) ||
					type.equals(OntologicalClasses.DBPEDIA_PERSON));
			//assert the entity-mention
			PlainLiteral mention = assertSingleValue(graph, body, FAM.entity_mention, PlainLiteral.class);
			assertEquals(CONTENT_LANGUAGE, mention.getLanguage());
			int start = CONTENT.indexOf(mention.getLexicalForm());
			assertTrue(start > 0);
			//assert the selector URI
			UriRef selector = assertSingleValue(graph, body, FAM.selector, UriRef.class);
			UriRef expectedSelectorUri = new UriRef(
					contentItem.getUri().getUnicodeString() + "#char=" +
					start+ "," + (start + mention.getLexicalForm().length()));
			assertEquals(expectedSelectorUri, selector);
			Set<UriRef> types = assertHasValues(graph, body, RDF_TYPE, UriRef.class);
			if(types.contains(FAM.EntityLinkingChoice)){ //in case this has suggestions
				assertTrue(types.contains(OA_CHOICE)); //check the type
				//an that there are suggestions
				assertHasValues(graph, body, OA_ITEM, NonLiteral.class);
			} else {
				assertNoValue(graph, body, OA_ITEM);
			}
			num++;
		}
		assertEquals(NUM_ENTITY_MENTIONS, num);
		num=0;

		it = graph.filter(null, RDF_TYPE, FAM.EntityAnnotation);
		while(it.hasNext()){
			NonLiteral body = it.next().getSubject();
			assertAnnotation(graph, body);
			Set<UriRef> types = assertHasValues(graph, body, RDF_TYPE, UriRef.class);
			Set<UriRef> selectors = new HashSet<UriRef>();
			selectors.addAll(assertOptValues(graph, body, FAM.selector, UriRef.class));
			if(types.contains(FAM.EntitySuggestion)){
				Set<NonLiteral> mentions = assertHasInvValues(graph, body, OA_ITEM, NonLiteral.class);
				for(NonLiteral mention : mentions){
					Set<UriRef> mTypes = assertHasValues(graph, mention, RDF_TYPE, UriRef.class);
					assertTrue(mTypes.contains(OA_CHOICE));
					assertTrue(mTypes.contains(FAM.EntityLinkingChoice));
					//collect the selectors
					selectors.addAll(assertOptValues(graph, mention, FAM.selector, UriRef.class));
				}
			} else if(types.contains(FAM.LinkedEntity)){
				assertTrue(types.contains(FAM.EntityMention));
				assertFalse(selectors.isEmpty());
			} else { //an Entity Annotation without a mention
				assertTrue(selectors.isEmpty());
			}
			PlainLiteral label = assertSingleValue(graph, body, FAM.entity_label, PlainLiteral.class);
			assertEquals(CONTENT_LANGUAGE, label.getLanguage());
			assertFalse(label.getLexicalForm().isEmpty());
			assertHasValues(graph, body, FAM.entity_type, UriRef.class);
			assertSingleValue(graph, body, FAM.entity_reference, UriRef.class);
			num++;
		}
		assertEquals(NUM_ENTITY_ANNOTATION, num);
		
		num = 0;
		it = graph.filter(null, RDF_TYPE, FAM.TopicClassification);
		while(it.hasNext()){
			NonLiteral body = it.next().getSubject();
			assertAnnotation(graph, body);
			//assert that also the oa:Composite type is used
			Set<UriRef> types = assertHasValues(graph, body, RDF_TYPE, UriRef.class);
			assertTrue(types.contains(OA_COMPOSITE));
			assertOptValue(graph, body, FAM.classification_scheme, UriRef.class);
			Set<NonLiteral> topics = assertHasValues(graph, body, OA_ITEM, NonLiteral.class);
			for(NonLiteral topic : topics){ //check that all items do use the fam:TopicAnnotation type
				Set<UriRef> tTypes = assertHasValues(graph, topic, RDF_TYPE, UriRef.class);
				assertTrue(tTypes.contains(FAM.TopicAnnotation));
			}
			num++;
		}
		assertEquals(NUM_TOPIC_CLASSIFICATION, num);
		
		num = 0;
		it = graph.filter(null, RDF_TYPE, FAM.TopicAnnotation);
		while(it.hasNext()){
			NonLiteral body = it.next().getSubject();
			assertAnnotation(graph, body);
			//assert that this topic annotation is part of an fam:TopicClassification
			NonLiteral topicClassification = assertSingleInvValue(graph, body, OA_ITEM, NonLiteral.class);
			Set<UriRef> tcTypes = assertHasValues(graph, topicClassification, RDF_TYPE, UriRef.class);
			assertTrue(tcTypes.contains(OA_COMPOSITE));
			assertTrue(tcTypes.contains(FAM.TopicClassification));
			//assert the topic
			PlainLiteral label = assertSingleValue(graph, body, FAM.topic_label, PlainLiteral.class);
			assertFalse(label.getLexicalForm().isEmpty());
			//assert the topic Reference
			assertOptValue(graph, body, FAM.topic_reference, UriRef.class);
			
			//check that the selector is the same as for the topic classification
			UriRef tcSelector = assertOptValue(graph, topicClassification, FAM.selector, UriRef.class);
			UriRef selector = assertOptValue(graph, body, FAM.selector, UriRef.class);
			assertEquals(selector, tcSelector); //NOTE: both might be null
			num++;
			
		}
		assertEquals(NUM_TOPIC_ANNOTATION, num);
	}

	/**
	 * This asserts that the parsed Body and Selector (optional) are correctly
	 * linked with an oa:Annotation and a oa:SpecialResource. It also checks that
	 * the fam:extractedFrom and fam:selector properties do link to the same resources
	 * as the oa:hasSource and oa:hasSelector values of the oa:SpecialResoruce
	 * @param graph
	 * @param annoBodyRes
	 * @param object
	 */
	private void assertAnnotation(TripleCollection graph, Resource annoBodyRes) {
		assertTrue(annoBodyRes instanceof NonLiteral);
		NonLiteral body = (NonLiteral)annoBodyRes;
		Set<UriRef> bodySelector = assertOptValues(graph, body, FAM.selector, UriRef.class);
		if(!engine.transformMetadata){ //no Metadata serialized ...
				assertNoInvValue(graph,body,OA_HAS_BODY); //no metadata expected
		} else { //assert all the metadata
			TypedLiteral confidence = assertOptValue(graph, body, FAM.confidence, TypedLiteral.class);
			if(confidence != null){
				assertTrue(confidence.getDataType().equals(XSD.double_) ||
						confidence.getDataType().equals(XSD.float_));
			}
			
			
			//Assert the annotation
			NonLiteral annotation = assertSingleInvValue(graph, body, OA_HAS_BODY, NonLiteral.class);
			TypedLiteral serializedAt = assertSingleValue(graph, annotation, OA_SERIALIZED_AT, TypedLiteral.class);
			assertEquals(XSD.dateTime, serializedAt.getDataType());
			Literal serializedBy = assertSingleValue(graph, annotation, OA_SERIALIZED_BY, Literal.class);
			assertEquals(engine.getClass().getName(), serializedBy.getLexicalForm());
			Collection<TypedLiteral> annotatedAt = assertHasValues(graph, annotation, OA_ANNOTATED_AT, TypedLiteral.class);
			for(TypedLiteral value : annotatedAt){
				assertEquals(XSD.dateTime, value.getDataType());
			}
			Collection<Literal> annotatedBy = assertHasValues(graph, annotation, OA_ANNOTATED_BY, Literal.class);
			for(Literal value : annotatedBy){
				assertTrue(!value.getLexicalForm().isEmpty());
			}
			
			NonLiteral spTarget = assertSingleValue(graph, annotation, OA_HAS_TARGET, NonLiteral.class);
			
			//source and extracted form need to be the same as the URI of the ContentItem
			UriRef source = assertSingleValue(graph, spTarget, OA_HAS_SOURCE, UriRef.class);
			assertEquals(contentItem.getUri(), source);
			UriRef extractedFrom = assertSingleValue(graph, body,FAM.extracted_from, UriRef.class);
			assertEquals(contentItem.getUri(), extractedFrom);
			
			//if a selector is present it must be the same referenced by both body and sptarget
			Set<UriRef> sptSelector = assertOptValues(graph, spTarget, OA_HAS_SELECTOR, UriRef.class);
			assertEquals(sptSelector, bodySelector);
		}
		//assert the selectors (present even if no metadata are serialized)
		for(UriRef selector : bodySelector){
			if(engine.selectorType == SelectorTypes.NIF || engine.selectorType == SelectorTypes.BOTH){
				assertNifSelector(graph, selector);
			}
			if(engine.selectorType == SelectorTypes.OA || engine.selectorType == SelectorTypes.BOTH) {
				assertOASelector(graph, selector);
			}
		}
	}

	private void assertSelectorUri(UriRef selector, int start, int end){
		UriRef expected = new UriRef(new StringBuilder(contentItem.getUri().getUnicodeString()).
				append("#char=").append(start).append(',').append(end).toString());
		assertEquals(expected, selector);
		
	}
	
	private void assertNifSelector(TripleCollection graph, UriRef selector){
		//assert the types
		Set<UriRef> types = assertHasValues(graph, selector, RDF_TYPE, UriRef.class);
		assertTrue(types.contains(NIF_STRING));
		assertTrue(types.contains(NIF_RFC5147STRING));
		
		//assert begin/end
		TypedLiteral beginIndex = assertSingleValue(graph, selector, NIF_BEGIN_INDEX, TypedLiteral.class);
		assertEquals(XSD.int_, beginIndex.getDataType());
		int begin = lf.createObject(Integer.class, beginIndex);
		TypedLiteral endIndex = assertSingleValue(graph, selector, NIF_END_INDEX, TypedLiteral.class);
		assertEquals(XSD.int_, endIndex.getDataType());
		int end = lf.createObject(Integer.class, endIndex);
		assertTrue(end > begin);
		
		//assert the selector URI
		assertSelectorUri(selector, begin, end);
		
		//assert anchor OR head/tail
		PlainLiteral anchor = assertOptValue(graph, selector, NIF_ANCHOR_OF, PlainLiteral.class);
		if(anchor != null){
			assertEquals(CONTENT_LANGUAGE, anchor.getLanguage());
			assertEquals(CONTENT.substring(begin,end), anchor.getLexicalForm());
		}
		PlainLiteral head = assertOptValue(graph, selector, NIF_HEAD, PlainLiteral.class);
		if(head != null){
			assertEquals(CONTENT_LANGUAGE, head.getLanguage());
			assertTrue(CONTENT.substring(begin,end).startsWith(head.getLexicalForm()));
		} else {
			assertNotNull("If no nif:anchor is presnet nif:head AND nif:tail MUST BE defined!", anchor);
		}
		PlainLiteral tail = assertOptValue(graph, selector, NIF_TAIL, PlainLiteral.class);
		if(tail != null){
			assertNotNull("if nif:tail is present also nif:head is expected!",head);
			assertEquals(CONTENT_LANGUAGE, tail.getLanguage());
			assertTrue(CONTENT.substring(end,CONTENT.length()).startsWith(tail.getLexicalForm()));
		} else {
			assertNull("if nif:head is present als nif:tail is expected!",head);
		}
		
		//assert before/after
		PlainLiteral before = assertOptValue(graph, selector, NIF_BEFORE, PlainLiteral.class);
		if(before != null){
			assertEquals(CONTENT_LANGUAGE, before.getLanguage());
			assertTrue(CONTENT.substring(0,begin).endsWith(before.getLexicalForm()));
		}
		PlainLiteral after = assertOptValue(graph, selector, NIF_AFTER, PlainLiteral.class);
		if(after != null){
			assertNotNull("if nif:after is present also nif:before is expected!",before);
			assertEquals(CONTENT_LANGUAGE, after.getLanguage());
			assertTrue(CONTENT.substring(end,CONTENT.length()).startsWith(after.getLexicalForm()));
		} else {
			assertNull("if nif:before is present als nif:after is expected!",before);
		}
		
		//assert the context
		UriRef context = assertSingleValue(graph, selector, NIF_REFERENCE_CONTEXT, UriRef.class);
		Set<UriRef> cTypes = assertHasValues(graph, context, RDF_TYPE, UriRef.class);
		assertTrue(cTypes.contains(NIF_CONTEXT));
		assertTrue(cTypes.contains(NIF_RFC5147STRING));
		UriRef sourceURL = assertSingleValue(graph, context, NIF_SOURCE_URL, UriRef.class);
		assertEquals(contentItem.getUri(), sourceURL);
		PlainLiteral isString = assertOptValue(graph, context, NIF_IS_STRING, PlainLiteral.class);
		if(isString != null){
			assertEquals(CONTENT_LANGUAGE, isString.getLanguage());
			assertEquals(CONTENT, isString.getLexicalForm());
		}
	}
	
	private void assertOASelector(TripleCollection graph, UriRef selector){
		Set<UriRef> types = assertHasValues(graph, selector, RDF_TYPE, UriRef.class);
		assertTrue(types.contains(OA_TEXT_POSITION_SELECTOR));
		assertTrue(types.contains(OA_TEXT_QUOTE_SELECTOR));
		
		TypedLiteral startIndex = assertSingleValue(graph, selector, OA_START, TypedLiteral.class);
		assertEquals(XSD.int_, startIndex.getDataType());
		int start = lf.createObject(Integer.class, startIndex);
		TypedLiteral endIndex = assertSingleValue(graph, selector, OA_END, TypedLiteral.class);
		assertEquals(XSD.int_, endIndex.getDataType());
		int end = lf.createObject(Integer.class, endIndex);
		assertTrue(end > start);
		
		//assert the selector URI
		assertSelectorUri(selector, start, end);

		PlainLiteral exact = assertSingleValue(graph, selector, OA_EXACT, PlainLiteral.class);
		if(exact != null){
			assertEquals(CONTENT_LANGUAGE, exact.getLanguage());
			assertEquals(CONTENT.substring(start,end), exact.getLexicalForm());
		}
		PlainLiteral prefix = assertSingleValue(graph, selector, OA_PREFIX, PlainLiteral.class);
		if(prefix != null){
			assertEquals(CONTENT_LANGUAGE, prefix.getLanguage());
			assertTrue(CONTENT.substring(0,start).endsWith(prefix.getLexicalForm()));
		}
		
		PlainLiteral suffix = assertSingleValue(graph, selector, OA_SUFIX, PlainLiteral.class);
		if(suffix != null){
			assertNotNull("if oa:suffix is present also oa:prefix is expected!",prefix);
			assertEquals(CONTENT_LANGUAGE, suffix.getLanguage());
			assertTrue(CONTENT.substring(end,CONTENT.length()).startsWith(suffix.getLexicalForm()));
		} else {
			assertNull("if oa:prefix is present als oa:suffix is expected!",prefix);
		}
		
	}
	
	private void assertNoValue(TripleCollection graph, NonLiteral subject,
			UriRef property) {
		Iterator<Triple> it = graph.filter(subject, property, null);
		assertFalse(it.hasNext());
	}

	private void assertNoInvValue(TripleCollection graph, Resource object,
			UriRef property) {
		Iterator<Triple> it = graph.filter(null, property, object);
		assertFalse(it.hasNext());
	}

	private <T extends Resource> Set<T> assertHasValues(TripleCollection graph,
			NonLiteral subject, UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(subject, property, null);
		assertTrue("missing value for property "+property+ "on subject "+subject, it.hasNext());
		Set<T> values = new HashSet<T>();
		while(it.hasNext()){
			Resource value = it.next().getObject();
			assertTrue(type.getSimpleName()+" expected but value "+ value +
					" had the type "+value.getClass().getSimpleName()+"!",
					type.isAssignableFrom(value.getClass()));
			values.add(type.cast(value));
		}
		return values;
	}
	private <T extends Resource> Set<T> assertHasInvValues(TripleCollection graph, NonLiteral object,
			UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(null, property, object);
		assertTrue("missing incoming value for property "+property+ "on object "+object, it.hasNext());
		Set<T> values = new HashSet<T>();
		while(it.hasNext()){
			Resource value = it.next().getSubject();
			assertTrue(type.getSimpleName()+" expected but value "+ value +
					" had the type "+value.getClass().getSimpleName()+"!",
					type.isAssignableFrom(value.getClass()));
			values.add(type.cast(value));
		}
		return values;
	}

	private <T extends Resource> Set<T> assertOptValues(TripleCollection graph,
			NonLiteral subject, UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(subject, property, null);
		if(!it.hasNext()){
			return Collections.emptySet();
		}
		Set<T> values = new HashSet<T>();
		while(it.hasNext()){
			Resource value = it.next().getObject();
			assertTrue(type.getSimpleName()+" expected but value "+ value +
					" had the type "+value.getClass().getSimpleName()+"!",
					type.isAssignableFrom(value.getClass()));
			values.add(type.cast(value));
		}
		return values;
	}

	private <T extends Resource> T assertSingleValue(TripleCollection graph, NonLiteral subject, UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(subject, property, null);
		assertTrue("missing value for property "+property+ "on subject "+subject, it.hasNext());
		Resource value = it.next().getObject();
		assertFalse("multi values for property "+property+ "on subject "+subject, it.hasNext());
		assertTrue(type.getSimpleName()+" expected but was "+value.getClass().getSimpleName()+"!",
				type.isAssignableFrom(value.getClass()));
		return type.cast(value);
	}

	private <T extends Resource> T assertOptValue(TripleCollection graph, NonLiteral subject, UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(subject, property, null);
		if(!it.hasNext()){
			return null;
		}
		Resource value = it.next().getObject();
		assertFalse("multi values for property "+property+ "on subject "+subject, it.hasNext());
		assertTrue(type.getSimpleName()+" expected but was "+value.getClass().getSimpleName()+"!",
				type.isAssignableFrom(value.getClass()));
		return type.cast(value);
	}
	
	private <T extends Resource> T assertSingleInvValue(TripleCollection graph, Resource object, UriRef property, Class<T> type) {
		Iterator<Triple> it = graph.filter(null, property, object);
		assertTrue("missing value for property "+property+ "on object "+object, it.hasNext());
		Resource value = it.next().getSubject();
		assertFalse("multi values for property "+property+ "on object "+object, it.hasNext());
		assertTrue(type.getSimpleName()+" expected but was "+value.getClass().getSimpleName()+"!",
				type.isAssignableFrom(value.getClass()));
		return type.cast(value);
	}
	
	private static void logRdf(String title, TripleCollection graph) {
		if(log.isDebugEnabled()){
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			rdfSerializer.serialize(out, graph, SupportedFormat.TURTLE);
			try {
				log.debug("{} {}",title == null ? "RDF:\n" : title, out.toString("UTF8"));
			} catch (UnsupportedEncodingException e) {/*ignore*/}
		}
	}

}
