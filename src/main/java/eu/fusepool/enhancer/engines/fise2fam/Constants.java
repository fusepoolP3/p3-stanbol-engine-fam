package eu.fusepool.enhancer.engines.fise2fam;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;

import eu.fusepool.p3.vocab.FAM;

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
    public static final UriRef NIF_RFC5147STRING = new UriRef(NS_NIF + "RFC5147String");
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
    public static final UriRef FISE_SENTIMENT = new UriRef(NamespaceEnum.fise + "sentiment");
    public static final UriRef FISE_SENTIMENT_TYPE = new UriRef(NamespaceEnum.fise + "Sentiment");
    public static final UriRef FISE_DOCUMENT_SENTIMENT = new UriRef(NamespaceEnum.fise + "DocumentSentiment");
    public static final UriRef FISE_KEYWORD_ANNOTATION = new UriRef(NamespaceEnum.fise + "KeywordAnnotation");
    public static final UriRef FISE_KEYWORD = new UriRef(NamespaceEnum.fise + "keyword");
    public static final UriRef FISE_COUNT = new UriRef(NamespaceEnum.fise + "count");
    
    
    //new FAM classes
    //NOTE: we can not use newer version of the p3-vocap because we need to use the old Clerezza version
    public static final String NS_FAM = "http://vocab.fusepool.info/fam#";
    /**
     * The <code>rdf:type</code> for <code>oa:body</code> used to annotate extracted sentiments.
     * Sentiment annotation may be about sections of the document. Sentiment annotations for the
     * whole document are expected to also use the {@link #FAM_DOCUMENT_SENTIMENT_ANNOTATION} type. 
     */
    public static final UriRef FAM_SENTIMENT_ANNOTATION = new UriRef(NS_FAM + "SentimentAnnotation");
    /**
     * Marks the sentiment for the document as a whole. 
     * A <code>rdfs:subClassOf</code> {@link #FAM_SENTIMENT_ANNOTATION}
     */
    public static final UriRef FAM_DOCUMENT_SENTIMENT_ANNOTATION = new UriRef(NS_FAM + "DocumentSentimentAnnotation");
    /**
     * The <code>fam:sentiment</code> value as a <code>xsd:double</code> in the range
     * [-1..1].
     */
    public static final UriRef FAM_SENTIMENT = new UriRef(NS_FAM + "sentiment");
    /**
     * A keyword detected in the processed document.
     */
    public static final UriRef FAM_KEYWORD_ANNOTATION = new UriRef(NS_FAM + "KeywordAnnotation");
    /**
     * The keyword
     */
    public static final UriRef FAM_KEYWORD = new UriRef(NS_FAM + "keyword");
    /**
     * the metric for the extracted keyword a <code>xsd:double</code> in the range [0..1]
     */
    public static final UriRef FAM_METRIC = new UriRef(NS_FAM + "metric");
    /**
     * the number of times the keyword appears in the text. For multi-word keywords
     * this number migt include mentions of sub sections. An <code>xsd:int</code> 
     * <code>&gt;= 1</code>.
     */
    public static final UriRef FAM_COUNT = new UriRef(NS_FAM + "count");
    
}
