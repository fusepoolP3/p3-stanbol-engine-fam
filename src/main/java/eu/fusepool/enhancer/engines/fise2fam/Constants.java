package eu.fusepool.enhancer.engines.fise2fam;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;

public interface Constants {

	static final String ANNO_URI_SUFFIX = "-annotation";
	static final int ANNO_URI_SUFFIX_LENGTH  = ANNO_URI_SUFFIX.length();
	
	static final String SPTARGET_URI_SUFFIX = "-sptarget";
	static final int SPTARGET_URI_SUFFIX_LENGTH = SPTARGET_URI_SUFFIX.length();
	
	public static final String NS_OA = "http://www.w3.org/ns/oa#";
	
	public static final UriRef OA_ANNOTATION = new UriRef(NS_OA + "Annotation");
	public static final UriRef OA_ANNOTATED_AT = new UriRef(NS_OA + "annotatedAt");
	public static final UriRef OA_ANNOTATED_BY = new UriRef(NS_OA + "annotatedBy");
	public static final UriRef OA_SERIALIZED_AT = new UriRef(NS_OA + "serializedAt");
	public static final UriRef OA_SERIALIZED_BY = new UriRef(NS_OA + "serializedBy");
	
	public static final UriRef OA_SPECIFIC_RESOURCE = new UriRef(NS_OA + "SpecificResource");
	public static final UriRef OA_HAS_BODY = new UriRef(NS_OA + "hasBody");
	public static final UriRef OA_HAS_TARGET = new UriRef(NS_OA + "hasTarget");
	public static final UriRef OA_HAS_SOURCE = new UriRef(NS_OA + "hasSource");
	public static final UriRef OA_HAS_SELECTOR = new UriRef(NS_OA + "hasSelector");
	
	public static final UriRef OA_TEXT_POSITION_SELECTOR = new UriRef(NS_OA + "TextPositionSelector");
	public static final UriRef OA_TEXT_QUOTE_SELECTOR = new UriRef(NS_OA + "TextQuoteSelector");
	public static final UriRef OA_START = new UriRef(NS_OA + "start");
	public static final UriRef OA_END = new UriRef(NS_OA + "end");
	public static final UriRef OA_EXACT = new UriRef(NS_OA + "exact");
	public static final UriRef OA_PREFIX = new UriRef(NS_OA + "prefix");
	public static final UriRef OA_SUFIX = new UriRef(NS_OA + "suffix");
	
	public static final UriRef OA_CHOICE = new UriRef(NS_OA + "Choice");
	public static final UriRef OA_COMPOSITE = new UriRef(NS_OA + "Composite");
	public static final UriRef OA_ITEM = new UriRef(NS_OA + "item");
	
	
	public static final String NS_NIF = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
	public static final UriRef NIF_STRING = new UriRef(NS_NIF + "String");
	public static final UriRef NIF_RFC5147STRING = new UriRef(NS_NIF + "RDF5147String");
	public static final UriRef NIF_CONTEXT = new UriRef(NS_NIF + "Context");
	public static final UriRef NIF_SOURCE_URL = new UriRef(NS_NIF + "sourceURL");
	public static final UriRef NIF_IS_STRING = new UriRef(NS_NIF + "isString");
	public static final UriRef NIF_BEGIN_INDEX = new UriRef(NS_NIF + "beginIndex");
	public static final UriRef NIF_END_INDEX = new UriRef(NS_NIF + "endIndex");
	public static final UriRef NIF_ANCHOR_OF = new UriRef(NS_NIF + "anchorOf");
	public static final UriRef NIF_HEAD = new UriRef(NS_NIF + "head");
	public static final UriRef NIF_TAIL = new UriRef(NS_NIF + "tail");
	public static final UriRef NIF_BEFORE = new UriRef(NS_NIF + "before");
	public static final UriRef NIF_AFTER = new UriRef(NS_NIF + "after");
	public static final UriRef NIF_REFERENCE_CONTEXT = new UriRef(NS_NIF + "referenceContext");
		
	
	public static final UriRef ENTITYHUB_SITE = new UriRef(NamespaceEnum.entityhub + "site");
	
	
}
