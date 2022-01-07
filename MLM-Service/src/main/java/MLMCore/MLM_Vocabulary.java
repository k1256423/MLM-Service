package MLMCore;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class MLM_Vocabulary {

	private static final String m_NS = "http://www.dke.jku.org/onto/mlm-vocabulary#";
	private static final String m_Prefix = "voc";
	

	public static Resource ModeledClass = createResource("ModeledClass");
    public static Resource OccurrenceClass = createResource("OccurrenceClass");
    public static Resource AbstractClass = createResource("AbstractClass");
    public static Resource ConcreteClass = createResource("ConcreteClass");    
    public static Resource InducedClass = createResource("InducedClass");
    
    public static Resource DomainObject = createResource("DomainObject");

    public static Property parOf = createProperty("partOf");
    public static Property of = createProperty("of");
    public static Property subContextOf = createProperty("subContextOf");
    public static Property uses = createProperty("uses");
    public static Property partiallyUses = createProperty("partiallyUses");
    public static Property modeledClass = createProperty("modeledClass");
    public static Property level = createProperty("level");
    
    //public static Property localTo = createProperty("localTo");
   
    private static Resource createResource(String name) {
        return ResourceFactory.createResource(m_NS + name);
    }

    private static Property createProperty(String name) {
        return ResourceFactory.createProperty(m_NS, name);
    }

    public static String getURI() { return m_NS; }
    public static String getPrefix() { return m_Prefix; }
}
