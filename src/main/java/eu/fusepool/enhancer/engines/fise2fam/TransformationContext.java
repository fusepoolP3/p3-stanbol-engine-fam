package eu.fusepool.enhancer.engines.fise2fam;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

public class TransformationContext {

	protected final ContentItem ci;
	protected final MGraph src;
	protected final MGraph tar;

	private Map<NonLiteral, Annotation> annotations = new HashMap<NonLiteral, Annotation>();
	
	private Set<UriRef> nifContexts;
	
	
	TransformationContext(ContentItem ci, MGraph target){
		if(ci == null){
			throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL!");
		}
		this.ci = ci;
		this.src = ci.getMetadata();
		this.tar = target == null ? new SimpleMGraph() : target;
	}

	/**
	 * Registers an annotation to the context
	 * @param anno te annotation
	 */
	protected void addAnnotation(Annotation anno){
		annotations.put(anno.enh, anno);
	}
	
	/**
	 * Looksup the Annotation for the <code>fise:Enhancement</code>
	 * @param enhancement the enhancement
	 * @return the {@link Annotation} or <code>null</code> if no one is
	 * registered for the parsed enhancement
	 */
	public Annotation getAnnotation(NonLiteral enhancement){
		return annotations.get(enhancement);
	}
	
	/**
	 * Used to keep track of NIF 2.0 <code>nif:Context</code> resources
	 * referenced by transformed enhancements. This is necessary as for each
	 * new <code>nif:Context</code> we need to add some additional triples. But
	 * as those contexts will be referenced multiple times we only want to do
	 * that the first time.
	 * @param context
	 * @return
	 */
	public boolean addNifContext(UriRef context){
		if(context == null){
			return false;
		}
		if(nifContexts == null){
			nifContexts = new HashSet<UriRef>();
		}
		return nifContexts.add(context);
	}
	
	public Set<UriRef> getNifContexts() {
		return nifContexts == null ? Collections.<UriRef>emptySet() : nifContexts;
	}
	
	/**
	 * Getter for the ContentItem
	 * @return
	 */
	public ContentItem getContentItem(){
		return ci;
	}
	/**
	 * The source graph
	 * @return
	 */
	public TripleCollection getSource() {
		return src;
	}
	
	public MGraph getTarget() {
		return tar;
	}

	public Collection<Annotation> getAnnotations() {
		return annotations.values();
	}
	
}
