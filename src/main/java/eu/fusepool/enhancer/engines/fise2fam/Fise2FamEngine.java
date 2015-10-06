/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.enhancer.engines.fise2fam;

import static eu.fusepool.enhancer.engines.fise2fam.Constants.*;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.get;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReferences;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DC_LINGUISTIC_SYSTEM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TOPICANNOTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DebugGraphics;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.p3.vocab.FAM;

/**
 * Post-processing {@link EnhancementEngine} that implements the conversion of
 * FISE enhancements to FAM as specified in 
 * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#transformation-of-fise-to-the-fusepool-annotation-model">
 * FISE to FAM tramsformation</a> section of the FAM specification
 * @author Rupert Westenthaler
 *
 */
@Component(immediate = true, metatype = true, 
configurationFactory = true, //TODO: check if multiple instances with different configurations do make sense
policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
    @Property(name= EnhancementEngine.PROPERTY_NAME,value="fise2fam"),
    @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class Fise2FamEngine extends AbstractEnhancementEngine<RuntimeException, RuntimeException> 
        implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private LiteralFactory lf = LiteralFactory.getInstance();

    /**
     * The types of selectors written by the Engine for transformed <code>fise:TextAnnotation</code>.
     * See the mapping specification as defined by the 
     * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#fisetextannotation-transformation">
     * <code>fise:TextAnnotation</code> transformation<a> section for detailed information.
     */
    public static enum SelectorTypes{
        /**
         * This mode will use <a href="http://persistence.uni-leipzig.org/nlp2rdf/">NIF 2.0</a>
         * <code>nif:String</code> based selectors.
         */
        NIF, 
        /**
         * This mode will use OpenAnnotation <a href="http://www.openannotation.org/spec/core/specific.html#TextPositionSelector">
         * Text Position Selectors</a> and <a href="http://www.openannotation.org/spec/core/specific.html#TextQuoteSelector">
         * Text Quote Selectors</a>
         */
        OA, 
        /**
         * This mode will write both {@link #NIF} and {@link #OA} selector information.
         * This created a lot of additional triples but might help with compatibility.
         */
        BOTH
    }
    
    /**
     * Property used to configure the ontology used for selectors. Supported types
     * are specified in the {@link SelectorTypes} enumeration. Only a single value
     * is allowed. String values are interpreted as {@link Enum#name()} and {@link Integer}
     * values as {@link Enum#ordinal()}.
     */
    @Property(options={
            @PropertyOption(name="NIF",value='%' + Fise2FamEngine.PROPERTY_SELECTOR_TYPE + ".option.nif"),
            @PropertyOption(name="OA",value='%' + Fise2FamEngine.PROPERTY_SELECTOR_TYPE + ".option.oa"),
            @PropertyOption(name="BOTH",value='%' + Fise2FamEngine.PROPERTY_SELECTOR_TYPE + ".option.both")
    }, value="NIF")
    public static final String PROPERTY_SELECTOR_TYPE = "enhancer.engine.fise2fam.selectortype";

    /**
     * By default the {@link #PROPERTY_SELECTOR_TYPE} is set to {@link SelectorTypes#NIF}
     */
    public static final SelectorTypes DEFAULT_SELECTOR_TYPE = SelectorTypes.NIF;
    
    /**
     * The types of selectors used to transform FISE selections to
     */
    protected SelectorTypes selectorType = DEFAULT_SELECTOR_TYPE;

    @Property(boolValue=Fise2FamEngine.DEFAULT_TRANSFORM_METADATA_STATE)
    public static final String PROPERTY_TRANSFORM_METADATA = "enhancer.engine.fise2fam.metadata";
    
    public static final boolean DEFAULT_TRANSFORM_METADATA_STATE = true;
    
    protected boolean transformMetadata = DEFAULT_TRANSFORM_METADATA_STATE;
    
    /**
     * Engines with lower ordering are executed later. As the transformation from
     * FISE enhancements to FAM should be after all the other engines this uses
     * <code>{@link Integer#MIN_VALUE}+10</code>.
     */
    private static final Integer DEFAULT_ORDERING = Integer.MIN_VALUE+10;
    
    private static final Map<String,Object> SERVICE_PROPERTIES = Collections.unmodifiableMap(
            Collections.<String,Object>singletonMap(ENHANCEMENT_ENGINE_ORDERING,DEFAULT_ORDERING));
    
    
    @Activate
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException{
        log.debug("> activate {}",getClass().getSimpleName());
        log.trace(" - config: {}",ctx.getProperties());
        super.activate(ctx);
        log.debug(" - name: {}",getName());
        //parse the configured selector type
        Object value = ctx.getProperties().get(PROPERTY_SELECTOR_TYPE);
        if(value instanceof String){
            try {
                selectorType = SelectorTypes.valueOf(value.toString());
            }  catch (IllegalArgumentException e){
                throw new ConfigurationException(PROPERTY_SELECTOR_TYPE, 
                        "The cofigured value '" + value + "' (type: " 
                        + value.getClass().getName() + ") is not valid. Allowed "
                        + "are " + Arrays.toString(SelectorTypes.values()) + "and "
                        + "ordinal numbers  [0.."+(SelectorTypes.values().length-1)
                        + "]parsed as Integers");
            }
        } else if(value instanceof Number){
            int ordinal = ((Number)value).intValue();
            if(ordinal >= SelectorTypes.values().length || ordinal < 0){
                throw new ConfigurationException(PROPERTY_SELECTOR_TYPE, 
                        "The cofigured value '" + value + "' (type: " 
                        + value.getClass().getName() + ") is not valid. Allowed "
                        + "are " + Arrays.toString(SelectorTypes.values()) + "and "
                        + "ordinal numbers  [0.."+(SelectorTypes.values().length-1)
                        + "]parsed as Integers");
            }
            selectorType = SelectorTypes.values()[ordinal];
        } else if (value != null){
            throw new ConfigurationException(PROPERTY_SELECTOR_TYPE, 
                    "The cofigured value '" + value + "' (type: " 
                    + value.getClass().getName() + ") is not supported. Supported"
                    + "are " + Arrays.toString(SelectorTypes.values()) + " parsed "
                    + "as Strings and ordinal numbers  [0.."
                    + (SelectorTypes.values().length-1) + "] parsed as Integers");
        } else { //not configured use the default
            selectorType = DEFAULT_SELECTOR_TYPE;
        }
        log.debug(" - selector type: {}",selectorType);
        
        value = ctx.getProperties().get(PROPERTY_TRANSFORM_METADATA);
        if(value instanceof Boolean){
            transformMetadata = ((Boolean)value).booleanValue();
        } else if(value != null){
            transformMetadata = Boolean.parseBoolean(value.toString());
        } else {
            transformMetadata = DEFAULT_TRANSFORM_METADATA_STATE;
        }
        log.debug(" - transform metadata: {}",transformMetadata);
        
    }
    
    @Deactivate
    @Override
    protected void deactivate(ComponentContext ctx) {
        log.debug("> deactivate {} (name: {})",getClass().getSimpleName(),getName());
        selectorType = DEFAULT_SELECTOR_TYPE;
        transformMetadata = DEFAULT_TRANSFORM_METADATA_STATE;
        super.deactivate(ctx);
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ServiceProperties#getServiceProperties()
     */
    @Override
    public Map<String, Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngine#canEnhance(org.apache.stanbol.enhancer.servicesapi.ContentItem)
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        //async access by other engines do not make any sense
        return EnhancementEngine.ENHANCE_SYNCHRONOUS;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngine#computeEnhancements(org.apache.stanbol.enhancer.servicesapi.ContentItem)
     */
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
    	long start = System.currentTimeMillis();
        TransformationContext ctx = new TransformationContext(ci, new SimpleMGraph());
        long base = System.nanoTime();
        long p1, p2, p3, p4;
        //(A) first pass: Main transformation work
        Iterator<Triple> enhIt = ctx.getSource().filter(null, RDF_TYPE, ENHANCER_ENHANCEMENT);
        while(enhIt.hasNext()){
            Annotation anno = new Annotation(ctx, enhIt.next().getSubject());
    		//(1) fise:Enhancement type specific mappings
            Set<UriRef> types = getRdfTypes(ctx.src,anno.enh);
            if(types.contains(ENHANCER_TEXTANNOTATION)){
            	transformTextAnnotation(anno);
            } else if(types.contains(ENHANCER_ENTITYANNOTATION)){
           		transformEntityAnnotation(anno);
            } else if(types.contains(ENHANCER_TOPICANNOTATION)){
            	transformTopicAnnotation(anno);
            } else if(types.contains(FISE_KEYWORD_ANNOTATION)){
                transformKeywordAnnotation(anno);
            } else {
            	log.warn("Unsupported fise:Enhancement type for enhancement {} (types: {})",
            			anno.enh,types);
            	continue; //This fise:Enhancement will stay!
            }

            //(2) General purpose attributes mapped for all fise:Enhancements
            //(2.a) confidence (if not yet written by the transform***Annotation(..) method)
            if(!ctx.tar.filter(anno.enh, FAM.confidence, null).hasNext()) {
                copyValue(ctx, anno.enh, ENHANCER_CONFIDENCE, anno.enh, FAM.confidence);
            }
            //(2.b) the fise extracted-from
    		copyValue(ctx, anno.enh, ENHANCER_EXTRACTED_FROM, anno.enh, FAM.extracted_from);

    		//(3) transformations that apply for all fise:Enhancements
            //(3.a) try to transform a selection
            UriRef selector = createSelectorRessource(anno);
            if(selector != null){ //a selection is present
	            if(selectorType == SelectorTypes.NIF || selectorType == SelectorTypes.BOTH){
	                transformSelection2Nif(anno, selector);
	            }
	            if(selectorType == SelectorTypes.OA || selectorType == SelectorTypes.BOTH){
	                transformSelection2OaSelector(anno, selector);
	            }
	            anno.setSelector(selector); //set the selector
            }
            //(3.b) transform the metadata (transformed as oa:Annotation and oa:SpecificResource)
            if(transformMetadata){
                transformEnhancement(anno);
            }
    		
            ctx.addAnnotation(anno); //add the transformed annotation to the context
        }
        p1 = System.nanoTime();
        
        //(B) 2nd pass: Selection phase
        for(Annotation anno : ctx.getAnnotations()){
        	//this connects the selector of the current annotation
        	//NOTE: could be also done in the first pass, but its good to kepp things
        	//      together.
        	if(anno.getSelector() != null){
        		ctx.tar.add(new TripleImpl(anno.getBody(), FAM.selector, anno.getSelector()));
        		if(anno.getSpTarget() != null) {//metadata are written
        			ctx.tar.add(new TripleImpl(anno.getSpTarget(),OA_HAS_SELECTOR, anno.getSelector()));
        		}
        	}
        	//This creates selectors based on selections of related annotations (e.g.
        	//an fam:EntitySuggestion linked with multiple fam:EntityMentions will get
        	//the selectors of all mentions. So one can directly see the relevant 
        	//sections of the text form the suggestion). 
        	for(NonLiteral related : anno.getRelated()){
        		Annotation relAnno = ctx.getAnnotation(related);
            	if(anno.getSelector() != null){
            		ctx.tar.add(new TripleImpl(anno.getBody(), FAM.selector, relAnno.getSelector()));
            		ctx.tar.add(new TripleImpl(anno.getSpTarget(),OA_HAS_SELECTOR, relAnno.getSelector()));
            	}
        	}
        }
        p2 = System.nanoTime();
        //(C) 3rd pass: We need to delete all fise:Enhancemnets from the
        //    metadata.
        //NOTE: one can not simple delete everything as this would also delete
        //    dereferenced Entities ...
    	MGraph metadata = ci.getMetadata();
        for(Annotation anno : ctx.getAnnotations()){
        	cleanFiseEnhancement(anno);
        	adaptNotTransformed(anno);
        }
        p3 = System.nanoTime();
        //(D): add the transformed Triples to the graph
        metadata.addAll(ctx.getTarget());
        p4 = System.nanoTime();
        long sum = p4-base;
        log.info(" ... transformed {} Annotations ({} triples) of ContentItem {} in {}ms",
        		new Object[]{ctx.getAnnotations().size(), ctx.tar.size(), 
        		ci.getUri(), System.currentTimeMillis()-start});
        if(log.isDebugEnabled()){
	        log.debug("  - p1: {}%| p2: {}%| p3: {}%| p4: {}%", new Object[]{
	        		((p1-base)*1000/sum)/10f,((p2-p1)*1000/sum)/10f,
	        		((p3-p2)*1000/sum)/10f,((p4-p3)*1000/sum)/10f});
        }
    }
    
    /**
     * Transformed annotation bodies typically do keep the same resource identifier
     * as the original <code>fise:Enhancement</code>. So links from not transformed
     * resources do keep valid. However in some cases (e.g. where two 
     * <code>fise:Enhancement</code>s are merged to a single annotation body) this
     * will be not the case.<p>
     * This methods inspects incoming links of the <code>fise:Enhancement</code>
     * in such cases and adapt those to the change resource identifier used by the
     * annotation body. Invalid links are removed from the source and the
     * corrected one are added to the target graph.
     * @param anno the annotation to check
     */
    private void adaptNotTransformed(Annotation anno) {
		//In case the fise:Enhancement do use a different resource as
		//Annotation Body
		if(!anno.getBody().equals(anno.enh)){
			//we need to redirect links of not transformed resources to the new
			//resource
			Iterator<Triple> incoming = anno.ctx.ci.getMetadata().filter(null, null, anno.enh);
			while(incoming.hasNext()){
				Triple t = incoming.next();
				if(anno.ctx.getAnnotation(t.getSubject()) == null){ //not transformed
					incoming.remove(); //remove the original triple
					//add a triple to the new Annotation Body resource to the target grpah
					anno.ctx.tar.add(new TripleImpl(t.getSubject(), t.getPredicate(), anno.getBody()));
				}
			}
		}
	}
    /**
     * Cleans triples in the metadata of the {@link ContentItem} of 
     * the transformed <code>fise:Enhancement</code>
     * @param anno the annotation to clean.
     */
	private void cleanFiseEnhancement(Annotation anno) {
		MGraph metadata = anno.ctx.ci.getMetadata();
    	//delete outgoing (incl. bNodes)
		List<NonLiteral> nodes = new ArrayList<NonLiteral>();
		nodes.add(anno.enh);
		while(!nodes.isEmpty()){
	    	Iterator<Triple> outgoing = metadata.filter(
	    			nodes.remove(nodes.size()-1), null, null);
			while(outgoing.hasNext()){
				Triple t = outgoing.next();
				if(t.getObject() instanceof BNode){
					nodes.add((BNode)t.getObject());
				}
				outgoing.remove();
			}
		}
	}

	private void transformTextAnnotation(Annotation anno) {
        //we need to distinquish different types of fise:TextAnnotations
        //(1) Language Annotations
        Set<UriRef> dcTypes = asSet(getReferences(anno.ctx.src, anno.enh, DC_TYPE));
        if (dcTypes.contains(DC_LINGUISTIC_SYSTEM)) { // this is a language annotation
            transformLanguageAnnotation(anno);
            return;
        }
        //(2) Sentiment Annotation
        //Sentiment Annotations do use ?enh dct:type fise:Sentiment
        if(dcTypes.contains(FISE_SENTIMENT_TYPE)){
            transformSentimentAnnotation(anno);
            return;
        }
        //(3) Topic Annotations
        Iterator<Triple> relation = anno.ctx.src.filter(null, DC_RELATION, anno.enh);
        while (relation.hasNext()) {
            NonLiteral related = relation.next().getSubject();
            if(hasValue(anno.ctx.src, related, RDF_TYPE, null, ENHANCER_TOPICANNOTATION)){
                transformTopicClassification(anno);
                return;
            }
        }
        //(4) Entity Mention Annotations (all remaining)
        transformEntityMentionAnnotation(anno);
    }
	
	/**
	 * Iterates over parsed values and adds them to a set
	 * @param it the value iterator
	 * @return the collected values as set. An empty set if <code>null</code> is
	 * parsed. 
	 */
    static <T> Set<T> asSet(Iterator<? extends T> it) {
        if(it == null || !it.hasNext()){
            return Collections.emptySet();
        }
        Set<T> set = new HashSet<T>();
        while(it.hasNext()){
            set.add(it.next());
        }
        return set;
    }

    /**
     * Implements mapping rules as defined by <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#famlanguageannotation-transformation">
     * <code>fam:LanguageAnnotation</code> transformation</a>
     * @param anno the Annotation to transform
     */
    private void transformLanguageAnnotation(Annotation anno) {
    	anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM.LanguageAnnotation));
        copyValue(anno.ctx, anno.enh, DC_LANGUAGE, anno.enh, DC_LANGUAGE);
        //the annotation body uses the same resource as the enhancement
        anno.setBody(anno.enh); 
    }

    /**
     * Implements mapping riles as defined by <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#famsentimenteannotation-transformation">
     * <code>fam:SentimentAnnotation</code> transformation</a>
     * @param anno the Annotation to transform
     */
    private void transformSentimentAnnotation(Annotation anno){
        anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM_SENTIMENT_ANNOTATION));
        copyValue(anno.ctx, anno.enh, FISE_SENTIMENT, anno.enh, FAM_SENTIMENT);
        //Give the sentiment for the Document a special type to make retrieval easier 
        if(anno.ctx.src.contains(new TripleImpl(anno.enh, DC_TYPE, FISE_DOCUMENT_SENTIMENT))){
            anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM_DOCUMENT_SENTIMENT_ANNOTATION));
        }
        EnhancementEngineHelper.getReferences(anno.ctx.src, anno.enh, DC_TYPE);
        //the annotation body uses the same resource as the enhancement
        anno.setBody(anno.enh); 
    }
    
    private void transformKeywordAnnotation(Annotation anno){
        anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM_KEYWORD_ANNOTATION));
        copyValue(anno.ctx, anno.enh, FISE_KEYWORD, anno.enh, FAM_KEYWORD);
        //the confidence of the enhancement is the metric for the keyword
        //so (1) we copy over the confidence to fam:metic
        copyValue(anno.ctx, anno.enh, ENHANCER_CONFIDENCE, anno.enh, FAM_METRIC);
        //(2) we do sent the confidence for Keyword annotation to 1.0
        EnhancementEngineHelper.set(anno.ctx.tar, anno.enh, FAM.confidence, lf.createTypedLiteral(1.0d));
        //(3) copy fise:count to fam:count
        copyValue(anno.ctx, anno.enh, FISE_COUNT, anno.enh, FAM_COUNT);
        //(4) add a confidence to the FAM annotation to 1.0
        anno.ctx.tar.add(new TripleImpl(anno.enh, FAM.confidence, lf.createTypedLiteral(1.0d)));
        //the annotation body uses the same resource as the enhancement
        anno.setBody(anno.enh); 
    }
    
    /**
     * Implements mapping rules as defined by <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#famtopicclassification-transformation">
     * <code>fam:TopicClassification</code> transformation</a>
     * @param anno the Annotation to transform
     */
    private void transformTopicClassification(Annotation anno) {
    	anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM.TopicClassification));
    	anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, OA_COMPOSITE));
        //the annotation body uses the same resource as the enhancement
        anno.setBody(anno.enh); 
    }

    /**
     * Implements mapping rules as defined by <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#famentitymention-transformation">
     * <code>fam:EntityMention</code> transformation</a>
     * @param anno the Annotation to transform
     */
    private void transformEntityMentionAnnotation(Annotation anno) {
    	anno.ctx.tar.add(new TripleImpl(anno.enh, RDF_TYPE, FAM.EntityMention));
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTED_TEXT, anno.enh , FAM.entity_mention);
        copyValue(anno.ctx, anno.enh, DC_TYPE, anno.enh, FAM.entity_type);
    }
    
	/**
     * Implements the transformation rules as specified by 
     * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#fiseentityannotation-transformation">
     * <code>fise:EntityAnnotation</code> transformation</a>
     * @param source
     * @param target
     * @param enh
     * @return
     */
    private void transformEntityAnnotation(Annotation anno) {
    	//TODO: This does currently not support creating fise:LinkedEntity
    	//      annotations in cases where only a single Entity is suggested
    	//      for a fise:TextAnnotation (fam:EntityMention)
    	//      supporting this would need an additional pass through already
    	//      transformed fise:Enthancements, as one needs to ensure that
    	//      all fise:TextAnnotation are already transformed
    	
		Iterator<Triple> mentions = anno.ctx.src.filter(anno.enh, DC_RELATION, null);
		while(mentions.hasNext()){
			Resource o = mentions.next().getObject();
			if(o instanceof NonLiteral){
				NonLiteral mention = (NonLiteral)o;
				if(hasValue(anno.ctx.src, mention, RDF_TYPE, null, ENHANCER_TEXTANNOTATION)){
					anno.addReated(mention); //add this as a related annotation
					//adapt EntityMention
					anno.ctx.tar.add(new TripleImpl(mention, RDF_TYPE, OA_CHOICE));
					anno.ctx.tar.add(new TripleImpl(mention, RDF_TYPE, FAM.EntityLinkingChoice));
					anno.ctx.tar.add(new TripleImpl(mention,OA_ITEM, anno.getBody()));
				}
			} //dc:relation to an Literal ... ignore
		}
		//add the RDF types to the entity annotation
    	anno.ctx.tar.add(new TripleImpl(anno.getBody(), RDF_TYPE, FAM.EntityAnnotation));
		if(!anno.getRelated().isEmpty()){ //if there is a fam:EntityMention linked
			//this is also a fam:EntitySuggestion
			anno.ctx.tar.add(new TripleImpl(anno.getBody(), RDF_TYPE, FAM.EntitySuggestion));
		}
		//direct mappings for fise:EntityAnnotation
		copyValue(anno.ctx, anno.enh, ENHANCER_ENTITY_REFERENCE, anno.getBody(), FAM.entity_reference);
		copyValue(anno.ctx, anno.enh, ENHANCER_ENTITY_LABEL, anno.getBody(), FAM.entity_label);
		copyValue(anno.ctx, anno.enh, ENHANCER_ENTITY_TYPE, anno.getBody(), FAM.entity_type);
		copyValue(anno.ctx, anno.enh, ENTITYHUB_SITE, anno.getBody(), ENTITYHUB_SITE);
	}

    private void transformTopicAnnotation(Annotation anno) {
		Iterator<Triple> relation = anno.ctx.src.filter(anno.enh, DC_RELATION, null);
		while(relation.hasNext()){
			Resource o = relation.next().getObject();
			if(o instanceof NonLiteral){
				NonLiteral related = (NonLiteral)o;
				if(hasValue(anno.ctx.src, related, RDF_TYPE, null, ENHANCER_TEXTANNOTATION)){
					anno.addReated(related); //add this as a related annotation
					anno.ctx.tar.add(new TripleImpl(related, OA_ITEM, anno.getBody()));
				} //else dc:relation to an none fise:TextAnnotation
			} //else dc:relation to an Literal ... ignore
		}
		if(!anno.getRelated().isEmpty()){
			anno.ctx.tar.add(new TripleImpl(anno.getBody(), RDF_TYPE, FAM.TopicAnnotation));
			copyValue(anno.ctx, anno.enh, ENHANCER_ENTITY_REFERENCE, anno.getBody(), FAM.topic_reference);
			copyValue(anno.ctx, anno.enh, ENHANCER_ENTITY_LABEL, anno.getBody(), FAM.topic_label);
			copyValue(anno.ctx, anno.enh, ENTITYHUB_SITE, anno.getBody(), ENTITYHUB_SITE);
			
		}//else ignore Topic Annotations without a Topic Classification
	}

    /**
     * Transforms an <code>fise:Enhancement</code> to an <code>oa:Annotation</code>
     * including the <code>oa:SpecialResource</code>. <i>NOTE:</i> that this
     * does NOT write relations to <code>oa:Selector</code> as at the time this
     * is executed not all selectors will be available. Selectors are done in a
     * 2nd pass when already all enhancements where transformed.<p>
     * Otherwise this implements the rules as defined in 
     * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#fiseenhancement-transformation">
     * fise:Enhancement transformation</a> section of the FAM specification.
     * @param source the {@link TripleCollection} with the FISE enhancements
     * @param target the {@link TripleCollection} to store the transformed triples to
     * @param enh the URI of the enhancement to transform
     * @param selector an optional selector the <code>oa:SpecialResource</code> should
     * link to
     * @return the node of the created <code>oa:Annotation</code>
     */
    private void transformEnhancement(Annotation annotation){
    	NonLiteral anno;
        NonLiteral sptarget;
        if(annotation.getAnnotation() == null){
	        if(annotation.enh instanceof UriRef){ //if the enhancement uses an URI
	            //ensure to use similar URIs for the OA Annoation and SpecificResource instances
	            String uri = ((UriRef)annotation.enh).getUnicodeString();
	            StringBuilder sb = new StringBuilder(uri.length()+ANNO_URI_SUFFIX_LENGTH);
	            anno = new UriRef(sb.append(uri).append(ANNO_URI_SUFFIX).toString());
	            sb = new StringBuilder(uri.length()+SPTARGET_URI_SUFFIX_LENGTH);
	            sptarget = new UriRef(sb.append(uri).append(SPTARGET_URI_SUFFIX).toString());
	        } else {
	            anno = new BNode();
	            sptarget = new BNode();
	        }
	        annotation.setAnnotation(anno); //set the oa:Annotation resource
	        annotation.setSpTarget(sptarget); //set the oa:SpecificRe
	        
	        //set properties only added when creating a new oa:Annotation resource
	        //add the rdf:type
	        annotation.ctx.tar.add(new TripleImpl(anno, RDF_TYPE, OA_ANNOTATION));
	        annotation.ctx.tar.add(new TripleImpl(anno, OA_SERIALIZED_AT, lf.createTypedLiteral(new Date())));
	        //TODO: use a real engine URI instead of the class name
	        annotation.ctx.tar.add(new TripleImpl(anno, OA_SERIALIZED_BY, lf.createTypedLiteral(getClass().getName())));
	        //add the relation between the body and the annotation
	        annotation.ctx.tar.add(new TripleImpl(anno, OA_HAS_BODY, annotation.getBody()));
	        //add anno -> sptarget
	        //NOTE: relations to selectors can only be written in a 2nd step (after
	        //      all enhancements where transformed)
	        annotation.ctx.tar.add(new TripleImpl(sptarget, RDF_TYPE, OA_SPECIFIC_RESOURCE));
	        annotation.ctx.tar.add(new TripleImpl(anno, OA_HAS_TARGET, sptarget));
	        annotation.ctx.tar.add(new TripleImpl(sptarget, OA_HAS_SOURCE, annotation.getExtractedFrom()));
        } else {
        	anno = annotation.getAnnotation();
        	sptarget = annotation.getSpTarget();
        }
        //copy the metadata that need also to be copied in case an additional
        //fise:Enhancement is added to an already existing oa:Annotation
        copyValue(annotation.ctx, annotation.enh, DC_CREATED, anno, OA_ANNOTATED_AT);
        copyValue(annotation.ctx, annotation.enh, DC_MODIFIED, anno, DC_MODIFIED);
        copyValue(annotation.ctx, annotation.enh, DC_CREATOR, anno, OA_ANNOTATED_BY);
        copyValue(annotation.ctx, annotation.enh, DC_CONTRIBUTOR, anno, OA_ANNOTATED_BY);
        
    }
    /**
     * Creates a <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a> encoded
     * Selector URI if the parsed annotation defines a selection. If not this
     * method returns <code>null</code>
     * @param anno the transformed annotation
     * @return the URI or <code>null</code> if no selection is present for the
     * parsed annotation
     */
	private UriRef createSelectorRessource(Annotation anno) {
		NonLiteral enh = anno.getEnhancement();
		Integer start = get(anno.ctx.src, enh, ENHANCER_START,Integer.class, lf);
		Integer end = get(anno.ctx.src, enh, ENHANCER_END,Integer.class, lf);
		if(start != null && end != null){
		    return createRFC5147URI(anno.getExtractedFrom(), start, end);
		} else {//no selection present
			return null;
		}
	}

	/**
     * Transforms a <code>fise:TextAnnotation</code> with a selected area of an text
     * to a <a href="http://persistence.uni-leipzig.org/nlp2rdf/">NIF 2.0</a>
     * <code>nif:String</code>. This method expects the parsed selector URI to
     * be encoded using <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a>
     * as encoding scheme<p>
     * The implementation of this method is according to the mapping specification
     * as defined by the <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#fisetextannotation-transformation">
     * <code>fise:TextAnnotation</code> transformation<a> section of the FAM model.
     * @param anno the annotation
     * @param selector the <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a>
     * encoded URI for the selector (typically created by using
     * {@link #createSelectorRessource(Annotation)})
     */
    private void transformSelection2Nif(Annotation anno, UriRef selector){
        anno.ctx.tar.add(new TripleImpl(selector, RDF_TYPE, NIF_STRING));
        anno.ctx.tar.add(new TripleImpl(selector, RDF_TYPE, NIF_RFC5147STRING));
        copyValue(anno.ctx, anno.enh, ENHANCER_START, selector, NIF_BEGIN_INDEX);
        copyValue(anno.ctx, anno.enh, ENHANCER_END, selector, NIF_END_INDEX);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTED_TEXT, selector, NIF_ANCHOR_OF);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_HEAD, selector, NIF_HEAD);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_TAIL, selector, NIF_TAIL);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_PREFIX, selector, NIF_BEFORE);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_SUFFIX, selector, NIF_AFTER);
        UriRef contextUri = createRFC5147URI(anno.getExtractedFrom(), null, null);
        if(anno.ctx.addNifContext(contextUri)){ //is this a new context
        	anno.ctx.tar.add(new TripleImpl(contextUri, RDF_TYPE, NIF_CONTEXT));
        	anno.ctx.tar.add(new TripleImpl(contextUri, RDF_TYPE, NIF_RFC5147STRING));
        	anno.ctx.tar.add(new TripleImpl(contextUri, NIF_SOURCE_URL, anno.getExtractedFrom()));
        	//NOTE: this does not add the contents of the ContentItem (nif:isString)
        }
        anno.ctx.tar.add(new TripleImpl(selector, NIF_REFERENCE_CONTEXT, contextUri));
    }
    
    /**
     * Transforms a <code>fise:TextAnnotation</code> with a selected area of an text
     * to an OpenAnnotation <a href="http://www.openannotation.org/spec/core/specific.html#TextPositionSelector">
     * Text Position Selector</a> and <a href="http://www.openannotation.org/spec/core/specific.html#TextQuoteSelector">
     * Text Quote Selector</a>. Typically a 
     * <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a> encoded 
     * selector URI is parsed. However while this is not a strict requirement.<p>
     * The implementation of this method is according to the mapping specification
     * as defined by the <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md#fisetextannotation-transformation">
     * <code>fise:TextAnnotation</code> transformation<a> section of the FAM model.
     * @param anno the transformed annotation
     * @param selector the selector resource. 
     */
    private void transformSelection2OaSelector(Annotation anno, UriRef selector){
        anno.ctx.tar.add(new TripleImpl(selector, RDF_TYPE, OA_TEXT_POSITION_SELECTOR));
        anno.ctx.tar.add(new TripleImpl(selector, RDF_TYPE, OA_TEXT_QUOTE_SELECTOR));
        copyValue(anno.ctx, anno.enh, ENHANCER_START, selector, OA_START);
        copyValue(anno.ctx, anno.enh, ENHANCER_END, selector, OA_END);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTED_TEXT, selector, OA_EXACT);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_HEAD, selector, ENHANCER_SELECTION_HEAD);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_TAIL, selector, ENHANCER_SELECTION_TAIL);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_PREFIX, selector, OA_PREFIX);
        copyValue(anno.ctx, anno.enh, ENHANCER_SELECTION_SUFFIX, selector, OA_SUFIX);
    }

    
    /**
     * Creates an <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a>
     * compatible URI. In case start or end is <code>null</code> a URI selecting
     * the whole document will be returned.
     * @param base the base URI
     * @param start the start position or <code>null</code> if the whole text is selected
     * @param end the end position or <code>null</code> ifthe whole text is selected
     * @return the RDC 5147 uri.
     */
    private UriRef createRFC5147URI(UriRef base, Integer start, Integer end){
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        if(start == null || end == null){
            sb.append("#char=0");
        } else {
            sb.append("#char=").append(start).append(',').append(end);
        }
        return new UriRef(sb.toString());
    }
    
    /**
     * Copies values from the source (graph, node, property) to the target (graph,
     * node, property).
     * @param ctx the {@link TransformationContext}
     * @param sourceNode the source node
     * @param sourceProp the source property
     * @param targetNode the target node
     * @param targetProp the target property
     * @return the number of values copied
     */
    private int copyValue(TransformationContext ctx, NonLiteral sourceNode,
            UriRef sourceProp, NonLiteral targetNode,
            UriRef targetProp) {
        Iterator<Triple> it = ctx.src.filter(sourceNode, sourceProp, null);
        int i = 0;
        while(it.hasNext()){
            Resource val = it.next().getObject();
            ctx.tar.add(new TripleImpl(targetNode,targetProp,val));
            i++;
        }
        return i;
    }

    /**
     * Simple helper method the returns the <code>fise:extracted-from</code> value
     * of an <code>fise:Enhancement</code>
     * @param source the triple collection with the FISE enhancements
     * @param enh the <code>fise:Enhancement</code>
     * @return the <code>fise:extracted-from</code> value or <code>null</code> if none
     */
    private UriRef getExtractedFrom(TripleCollection source, NonLiteral enh) {
        return getReference(source, enh, ENHANCER_EXTRACTED_FROM);
    }
    
    /**
     * Checks if the value is parsed of the parsed triple filter.
     * IMPARTANT: This method expects that exactly one of subject, predicate and
     * object is <code>null</code>
     * @param source the triple collection
     * @param sub subject filter (<code>null</code> for wildcard)
     * @param pred predicate filter (<code>null</code> for wildcard)
     * @param obj Object filter (<code>null</code> for wildcard)
     * @param value the value
     * @return <code>true</code> if the parsed value is part of the triples selected
     * by the parsed triple pattern.
     */
    public boolean hasValue(TripleCollection source, NonLiteral sub, UriRef pred, Resource obj, Resource value){
    	if(value == null){
    		return false;
    	}
    	Iterator<Triple> it = source.filter(sub, pred, obj);
    	while(it.hasNext()){
    		Triple t = it.next();
    		Resource act = sub == null ? t.getSubject() : pred == null 
    				? t.getPredicate() : t.getObject();
    		if(act.equals(value)){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Helper Method that returns the <code>rdf:type</code>s of the parsed resource 
     * as a set.
     * @param source
     * @param resoruce the resource
     * @return the <code>rdf:type</code>s of the parsed resource 
     */
    private Set<UriRef> getRdfTypes(TripleCollection source, NonLiteral resoruce) {
        return resoruce == null ? Collections.<UriRef>emptySet() : 
            asSet(EnhancementEngineHelper.getReferences(source, resoruce, RDF_TYPE));
    }
}
