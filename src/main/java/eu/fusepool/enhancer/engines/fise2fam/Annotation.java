package eu.fusepool.enhancer.engines.fise2fam;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * Data structure that keeps track of the different resources used to describe
 * an Annotation transformed from an Enhancement.
 * @author Rupert Westenthaler
 *
 */
public class Annotation {

    /**
     * The transformation context
     */
    protected final TransformationContext ctx;
    /**
     * The <code>fise:Enhancement</code>. Will be also used as uri for the
     * Annotation Body.
     */
    protected final NonLiteral enh;
    
    private NonLiteral body;

    private NonLiteral annotation;
    
    private NonLiteral spTarget;
    
    private UriRef _extractedFrom;
    
    private UriRef selector;
    
    private Set<NonLiteral> related;
    
    public Annotation(TransformationContext ctx, NonLiteral enh) {
        this.ctx = ctx;
        this.enh = enh;
    }
    
    public TransformationContext getContext() {
        return ctx;
    }
    
    public NonLiteral getEnhancement() {
        return enh;
    }

    public NonLiteral getBody() {
        return body == null ? enh : body;
    }

    public void setBody(NonLiteral body) {
		this.body = body;
	}
    
    public UriRef getExtractedFrom() {
        if(_extractedFrom == null){
            _extractedFrom = getReference(ctx.getSource(), 
                    enh, ENHANCER_EXTRACTED_FROM);
        }
        return _extractedFrom;
    }
    
    public void setSelector(UriRef selector) {
		this.selector = selector;
	}
    
    public UriRef getSelector() {
		return selector;
	}

    public void setAnnotation(NonLiteral annotation) {
		this.annotation = annotation;
	}
    
    public NonLiteral getAnnotation() {
		return annotation;
	}
    
    public void setSpTarget(NonLiteral spTarget) {
		this.spTarget = spTarget;
	}
    
    public NonLiteral getSpTarget() {
		return spTarget;
	}
    
    protected void addReated(NonLiteral enh){
    	if(enh == null){
    		return;
    	}
    	if(related == null){
    		related = new HashSet<NonLiteral>(4);
    	}
    	related.add(enh);
    }
    
    public Set<NonLiteral> getRelated() {
		return related == null ? Collections.<NonLiteral>emptySet() : related;
	}
    
	@Override
	public int hashCode() {
		return enh.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (getClass() != other.getClass())
			return false;
		return enh.equals(((Annotation)other).enh);
	}
	
	@Override
	public String toString() {
		return new StringBuilder("Annotation: ").append(enh).toString();
	}

}
