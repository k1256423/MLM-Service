package MLMCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

public class DomainModel {

	private static final String m_NS = "http://www.dke.jku.org/onto/mlph-kg#";
	private static final String m_Prefix = "kg";
	private Model m_model;
	private String m_strModelFile = "";
	private String m_strModelRoot = "data" + System.getProperty("file.separator") + "tdbStore";

	public DomainModel() {
		m_model = ModelFactory.createDefaultModel();
		m_model.setNsPrefixes(getPrefixes());
	}

	public DomainModel(String strFileName) {

		this();
		setModelFileName(strFileName);
		File file = new File(m_strModelFile);
		if (file.exists())
			m_model.read(m_strModelFile);
	}
	
	public void clearModel() {		
		m_model = ModelFactory.createDefaultModel();
		m_model.setNsPrefixes(getPrefixes());
	}
	
	public boolean deleteModel() {		
		File file = new File(m_strModelFile);
		if(file.exists()) {
			if(file.delete()) {
				clearModel();
				return true;
			}				
		}
		return false;
	}

	private PrefixMapping getPrefixes() {

		PrefixMapping prefixes = new PrefixMappingImpl();
		prefixes.setNsPrefix("rdf", RDF.getURI());
		prefixes.setNsPrefix("rdfs", RDFS.getURI());
		prefixes.setNsPrefix("sh", SHACL.getURI());
		prefixes.setNsPrefix("xsd", XSD.getURI());
		prefixes.setNsPrefix(MLM_Vocabulary.getPrefix(), MLM_Vocabulary.getURI());
		prefixes.setNsPrefix(m_Prefix, m_NS);
		return prefixes;
	}

	public Resource modelClass(String strClassName) {
		return modelClass(strClassName, null);
	}

	public Resource modelClass(String strClassName, Resource compositeModeledClass) {
		Resource modeledClass = m_model.createResource(m_NS + strClassName);
		m_model.add(modeledClass, RDF.type, MLM_Vocabulary.ModeledClass);
		if (compositeModeledClass != null)
			m_model.add(modeledClass, MLM_Vocabulary.parOf, compositeModeledClass);
		return modeledClass;
	}

	public Resource modelOccurrenceClass(String strClassName, Resource compositeModeledClass) {

		Resource occurrenceClass = modelClass(strClassName, compositeModeledClass);
		m_model.add(occurrenceClass, RDF.type, MLM_Vocabulary.OccurrenceClass);
		return occurrenceClass;
	}

	public Resource modelAbstarctClass(String strClassName, Resource superClass) {
		Resource abstractClass = modelClass(strClassName);
		m_model.add(abstractClass, RDF.type, MLM_Vocabulary.AbstractClass);
		m_model.add(abstractClass, RDFS.subClassOf, superClass);

		StmtIterator iterator = m_model.listStatements(superClass, MLM_Vocabulary.parOf, (RDFNode) null);
		if (iterator.hasNext()) {
			m_model.add(abstractClass, MLM_Vocabulary.parOf, iterator.next().getObject().asResource());
		}
		deriveRdfType(MLM_Vocabulary.OccurrenceClass);
		return abstractClass;
	}

	public Resource modelConcreteClass(String strClassName, Resource superClass) {
		Resource concereteClass = modelClass(strClassName);
		m_model.add(concereteClass, RDF.type, MLM_Vocabulary.ConcreteClass);
		m_model.add(concereteClass, RDFS.subClassOf, superClass);

		StmtIterator iterator = m_model.listStatements(superClass, MLM_Vocabulary.parOf, (RDFNode) null);
		if (iterator.hasNext()) {
			m_model.add(concereteClass, MLM_Vocabulary.parOf, iterator.next().getObject().asResource());
		}
		deriveRdfType(MLM_Vocabulary.OccurrenceClass);
		return concereteClass;
	}

	public Resource createObject(String strObjectName, Resource modeledClass) {
		Resource object = m_model.createResource(m_NS + strObjectName);
		m_model.add(object, RDF.type, MLM_Vocabulary.DomainObject);
		m_model.add(object, RDF.type, modeledClass);
		return object;
	}

	public Resource createObject(String strObjectName, Resource modeledClass, Resource compositeObject) {
		Resource object = createObject(strObjectName, modeledClass);
		m_model.add(object, MLM_Vocabulary.of, compositeObject);
		//m_model.add(object, RDF.type, getInducedClass(modeledClass, compositeObject));
		return object;
	}
	
	 public void createUse(Resource composite, Resource modeledClass) {
	        m_model.add(composite, MLM_Vocabulary.uses, modeledClass);
	    }

//	private Resource constructInducedClass(Resource modeledClass, Resource context) {
//		Resource inducedClass = m_model
//				.createResource(m_NS + modeledClass.getLocalName() + "(" + context.getLocalName() + ")");
//		return inducedClass;
//	}

	public void propagateModel() {
		
		int iIterations = 0;
		long lPreviousSize = 0;
		do {
			iIterations++;
			lPreviousSize = m_model.size();
			propagateSubContexts();
			propagateUses();
			propagatePartiallyUses();
			deriveInducesClasses();
			propagatePartOf();
			updateLevelProperty();
			
			//write2File("Iteration" + iIterations);
			System.out.println("propagateModel iterative change: " + iIterations);
			
		}while(lPreviousSize < m_model.size());				
	}

	private void propagateSubContexts() {
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "";
		
		strStatement = "INSERT { ?class <" + MLM_Vocabulary.subContextOf + "> ?superClass . }" +
					   "WHERE { ?class <" + RDFS.subClassOf + "> ?superClass . }";		
		statements.add(strStatement);

		strStatement = "INSERT { ?object <" + MLM_Vocabulary.subContextOf + "> ?class . }" +
					   "WHERE { ?object <" + RDF.type + "> <" + MLM_Vocabulary.DomainObject + "> ." +
				               "?object <" + RDF.type + "> ?class . " +
				               "?class <" + RDF.type + "> ?classType ." +
				               "Filter ( ?classType in (<" + MLM_Vocabulary.ModeledClass + ">, <" + MLM_Vocabulary.InducedClass + ">) ) ." +
					        " }";
		statements.add(strStatement);
	
		updateModel(statements);
	}
	
	private void propagateUses() {
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "";

		strStatement = "INSERT { ?x <" + MLM_Vocabulary.uses + "> ?c . }" +
					   "WHERE { ?y <" + MLM_Vocabulary.uses + "> ?c . " +					   		   
				               "?x <" + MLM_Vocabulary.subContextOf + ">+ ?y . " +
//				               "FILTER NOT EXISTS { ?c <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
//				               "FILTER NOT EXISTS { ?x <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
//				               "FILTER NOT EXISTS { ?y <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
					        " }";
		statements.add(strStatement);
		
		strStatement = "INSERT { ?x <" + MLM_Vocabulary.uses + "> ?d . }" +
				   	   "WHERE { ?x <" + MLM_Vocabulary.uses + "> ?c . " +
			                   "?c <" + RDFS.subClassOf + ">+ ?d . " +
//				               "FILTER NOT EXISTS { ?c <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
//				               "FILTER NOT EXISTS { ?d <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
//				               "FILTER NOT EXISTS { ?x <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . }" +
			                " }";
		statements.add(strStatement);
	
		updateModel(statements);
	}
	
	private void propagatePartiallyUses() {
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "";
		
		strStatement = "INSERT { ?x <" + MLM_Vocabulary.partiallyUses + "> ?c . }" +
				   	   "WHERE { ?x <" + MLM_Vocabulary.uses + "> ?c . }";
		statements.add(strStatement);		
		
		strStatement = "INSERT { ?y <" + MLM_Vocabulary.partiallyUses + "> ?c . }" +
				   	   "WHERE { ?x <" + MLM_Vocabulary.partiallyUses + "> ?c . " +
			               	   "?x <" + MLM_Vocabulary.subContextOf + ">+ ?y . " +
				        " }";
		statements.add(strStatement);	
		updateModel(statements);
	}
	
	private void propagatePartOf() {
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "INSERT { ?x <" + MLM_Vocabulary.parOf + "> ?c . }" +
							  "WHERE { ?x <" + MLM_Vocabulary.of + "> ?c . }";
		updateModel(strStatement);
	}
	
	private void deriveInducesClasses() {
		//wenn x partially-use c) gilt, dann wird eine lokale Klasse c(x) abgeleitet. Bereits bestehende Klassen e.g. participant(study) || AffectedParticipant(study)... werden nicht abgeleitet
		
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "";
		
		//derive induced classes
		strStatement = "INSERT { ?inducedClassName <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . " +
				                "?inducedClassName <" + MLM_Vocabulary.parOf + "> ?x . " +
				                "?inducedClassName <" + MLM_Vocabulary.modeledClass + "> ?c . " +
						      "}" +
					   "WHERE { ?x <" + MLM_Vocabulary.partiallyUses + "> ?c . " +
						       "?x <" + MLM_Vocabulary.subContextOf + ">+ ?d . " + 
						       "BIND(STRAFTER(STR(?c), STR(" + m_Prefix + ":)) AS ?className) ." +
							   "BIND(STRAFTER(STR(?x), STR(" + m_Prefix + ":)) AS ?compositeName) ." +
							   "BIND(IRI(STR(" + m_Prefix + ":) + ?className + \"(\" + ?compositeName + \")\") AS ?inducedClassName) ." +
							 "}";
		statements.add(strStatement);
		
		//derive subClassOf/subContextOf from modeled class
		strStatement = "INSERT { ?inducedClass <" + RDFS.subClassOf + "> ?inducedSuperClass . }" +
					   "WHERE { ?inducedClass <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . " +
					   		   "?inducedClass <" + MLM_Vocabulary.modeledClass + "> ?c . " +
					   		   "?c <" + MLM_Vocabulary.subContextOf + "> ?c_SuperClass . " + 
					   		   "?inducedClass <" + MLM_Vocabulary.parOf + "> ?x . " +
					   		   "BIND(STRAFTER(STR(?c_SuperClass), STR(" + m_Prefix + ":)) AS ?c_SuperClassName) ." +
					   		   "BIND(STRAFTER(STR(?x), STR(" + m_Prefix + ":)) AS ?x_Name) ." +
							   "BIND(IRI(STR(" + m_Prefix + ":) + ?c_SuperClassName + \"(\" + ?x_Name + \")\") AS ?inducedSuperClass) ." +					   		   
					   		 "}";
		statements.add(strStatement);
		
		//derive subClassOf/subContextOf from composite class (partOf - respectively context)
		strStatement = "INSERT { ?inducedClass <" + RDFS.subClassOf + "> ?inducedSuperClass . }" +
					   "WHERE { ?inducedClass <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . " +
					   		   "?inducedClass <" + MLM_Vocabulary.parOf + "> ?x . " + 
					   		   "?x <" + MLM_Vocabulary.subContextOf + "> ?x_SuperClass . " +
					   		   "?inducedClass <" + MLM_Vocabulary.modeledClass + "> ?c . " +
	
					   		   "FILTER EXISTS { ?x_SuperClass <" + RDFS.subClassOf + "> ?x_SuperSuperClass . } " + //?x_SuperSuperClass is used to prevent the derivation of subClassOf/subContextOf properties to root composite classes
					   		   "BIND(STRAFTER(STR(?x_SuperClass), STR(" + m_Prefix + ":)) AS ?x_SuperClassName) ." +
					   		   "BIND(STRAFTER(STR(?c), STR(" + m_Prefix + ":)) AS ?c_Name) ." +
					   		   "BIND(IRI(STR(" + m_Prefix + ":) + ?c_Name + \"(\" + ?x_SuperClassName + \")\") AS ?inducedSuperClass) ." +				   		   
					   		 "}";
		statements.add(strStatement);
		
		//derive subClassOf/subContextOf to root composite classes
		strStatement = "INSERT { ?inducedClass <" + RDFS.subClassOf + "> ?c . }" +
					   "WHERE { ?inducedClass <" + RDF.type + "> <" + MLM_Vocabulary.InducedClass + "> . " +
					   		   "?inducedClass <" + MLM_Vocabulary.parOf + "> ?x . " + 
					   		   "?x <" + MLM_Vocabulary.subContextOf + "> ?x_SuperClass . " +
					   		   "?inducedClass <" + MLM_Vocabulary.modeledClass + "> ?c . " +	
					   		   "FILTER NOT EXISTS { ?x_SuperClass <" + RDFS.subClassOf + "> ?x_SuperSuperClass . } " +			   		   
					   		 "}";
		statements.add(strStatement);		
		
		updateModel(statements);
	}
	
	private void updateLevelProperty() {
		
		ArrayList<String> statements = new ArrayList<String>();
		String strStatement = "";
	
		
		strStatement = "DELETE {?s <" + MLM_Vocabulary.level + "> ?o}" +
				   	   "WHERE { ?s <" + MLM_Vocabulary.level + "> ?o . }";
		statements.add(strStatement);
		
		strStatement = "INSERT { ?s <" + MLM_Vocabulary.level + "> ?level . }" +
					   "WHERE { " + 
								"SELECT ?s (count(*)+1 AS ?level)" +
								"WHERE { " +
										"?s <" + MLM_Vocabulary.parOf + ">+ ?composite . " +
								"}" +
								"GROUP BY ?s" +
							  "}";
		statements.add(strStatement);
		
		strStatement = "INSERT { ?s <" + MLM_Vocabulary.level + "> 1 . }" +
				   "WHERE { " + 
							"?s ?p ?o ." +
							"FILTER (?o  IN (<" + MLM_Vocabulary.ModeledClass + ">, <" + MLM_Vocabulary.InducedClass + ">, <" + MLM_Vocabulary.DomainObject + "> ))" +
							"Filter NOT EXISTS { ?s <" + MLM_Vocabulary.parOf + "> ?composite .  }" +							
						  "}";
	statements.add(strStatement);
	updateModel(statements);
	}
	
	private void deriveRdfType(Resource rdfType) {

		String strStatement = ("""
				INSERT {
				""" + "?class <" + RDF.type + "> <" + rdfType + "> . " + """
				}
				WHERE {
				""" + "?class <" + RDFS.subClassOf + ">+ ?superClass . " + "?superClass <" + RDF.type + "> <" + rdfType
				+ "> . " + """

						}
						""");
		updateModel(strStatement);
	}

	

	private void updateModel(String strUpdateStatement) {

		ArrayList<String> straUpdateStatements = new ArrayList<String>();
		straUpdateStatements.add(strUpdateStatement);
		updateModel(straUpdateStatements);
	}

	private void updateModel(ArrayList<String> straUpdateStatements) {

		UpdateRequest updateRequest = UpdateFactory.create();
		updateRequest.setPrefixMapping(getPrefixes());
		for (String strUpdateStatement : straUpdateStatements)
			updateRequest.add(strUpdateStatement);
		UpdateAction.execute(updateRequest, m_model);
	}

	public void print() {

		RDFDataMgr.write(System.out, m_model, Lang.TTL);
	}

	public void write2File() {

		if (m_strModelFile.length() > 0) {

			File file = new File(m_strModelRoot);
			if (!file.exists())
				file.mkdirs();
						
			///###backup file for development purposes			
			file = new File(m_strModelFile);
			if (file.exists()) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
				file.renameTo(new File(m_strModelFile.replace(".ttl", "-" + timeStamp +".ttl")));
			}
			//###

			try (OutputStream out = new FileOutputStream(m_strModelFile)) {
				RDFDataMgr.write(out, m_model, Lang.TTL);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write2File(String strFileName) {

		setModelFileName(strFileName);
		write2File();
	}
	
	private void setModelFileName(String strFileName) {
		m_strModelFile = m_strModelRoot + System.getProperty("file.separator") + strFileName;
		if (m_strModelFile.indexOf(".ttl") < 0)
			m_strModelFile += ".ttl";
	}

	public void printCount() {

		System.out.println(m_model.listStatements().toList().size());
	}

	public void printInducedClasses() {

		ArrayList<HashMap<String, String>> list = executeQuery("SELECT distinct ?s WHERE { ?s rdf:type <" + MLM_Vocabulary.InducedClass  + "> } ORDER BY ?s");
		System.out.println("InducedClasses:");
		for(HashMap<String, String> map : list) {
			System.out.println(map.get("s"));
		}			
	}

	public String getPrefixString() {

		return m_model.getNsPrefixMap().toString().replace("{", "PREFIX ").replace(",", "> PREFIX").replace("=", ": <")
				.replace("}", "> ");
	}

	private String substituteUriWithPrefix(Resource resource) {

		String strNode = resource.toString();
		Map<String, String> map = m_model.getNsPrefixMap();
		for (Map.Entry<String, String> prefix : map.entrySet()) {

			if (strNode.startsWith(prefix.getValue()))
				return strNode.replace(prefix.getValue(), prefix.getKey() + ":");
		}

		return strNode;
	}

	private ArrayList<HashMap<String, String>> executeQuery(String strQuery) {

		ResultSet resultSet = null;
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		strQuery = getPrefixString() + " " + strQuery;
		Query query = QueryFactory.create(strQuery);
		QueryExecution qexec = QueryExecutionFactory.create(query, m_model);
		try {
			resultSet = qexec.execSelect();

			List<String> straColumnNames = resultSet.getResultVars();
			while (resultSet.hasNext()) {
				QuerySolution row = resultSet.nextSolution();
				HashMap<String, String> resluts = new HashMap<String, String>();
				for (int i = 0; i < straColumnNames.size(); i++) {
					String strColumnName = straColumnNames.get(i);
					RDFNode rdfNode = row.get(strColumnName);
					if(rdfNode == null)
						resluts.put(strColumnName, "");
					else if (rdfNode.isResource())
						resluts.put(strColumnName, substituteUriWithPrefix(rdfNode.asResource()));
					else
						resluts.put(strColumnName, rdfNode.toString());
				}
				list.add(resluts);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage().toString());
		} finally {
			qexec.close();
		}

		return list;
	}

	public void writeForceDirectedGraphData2File(String strFileName) {
		PrintWriter writer = null;
		String strRoot = "data" + System.getProperty("file.separator") + "graphic";
		try {
			File file = new File(strRoot);
			if (!file.exists())
				file.mkdirs();
			if (strFileName.indexOf(".ttl") < 0)
				strFileName += ".ttl";
			strFileName = strRoot + System.getProperty("file.separator") + strFileName;
			writer = new PrintWriter(strFileName, "UTF-8");
			writer.println(getForceDirectedGraphModeledAndDerivedClasses());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	public String getForceDirectedGraphData() {

		List<String> straNodeList = new ArrayList<String>();
		String strSubject = "", strObject = "", strPredicate = "", strNodes = "", strLinks = "", strLevel = "";

		ArrayList<HashMap<String, String>> list = executeQuery("""
				select ?s (COUNT(?x) as ?level)
				WHERE { ?s voc:partOf+ ?x. }
				Group BY ?s
				""");

		for (int i = 0; i < list.size(); i++) {
			HashMap<?, ?> map = (HashMap<?, ?>) list.get(i);

			strSubject = map.get("s").toString();
			strLevel = map.get("level").toString();
			if (strLevel.indexOf("^^") >= 0)
				strLevel = strLevel.substring(0, strLevel.indexOf("^^"));

			if (!straNodeList.contains(strSubject)) {
				straNodeList.add(strSubject);
				if (strNodes.length() != 0)
					strNodes += "," + System.lineSeparator();
				strNodes += "{\"id\": \"" + strSubject + "\",";
				strNodes += "\"name\": \"" + strSubject + "\",";
				strNodes += "\"label\": \"" + strSubject + "\",";
				strNodes += "\"group\": \"Team A\",";
				strNodes += "\"runtime\": 20, \"category\":" + strLevel + "}";
			}
		}

		list = executeQuery(
				"""
						select *
						WHERE { ?s ?p ?o
								FILTER (?o NOT IN (voc:AbstractClass, voc:ModeledClass, voc:ConcreteClass, voc:OccurrenceClass, voc:DomainObject ))
						}
						ORDER BY ?s
						""");

		for (int i = 0; i < list.size(); i++) {
			HashMap<?, ?> map = (HashMap<?, ?>) list.get(i);

			strSubject = map.get("s").toString();
			strObject = map.get("o").toString();
			strPredicate = map.get("p").toString();

			if (!straNodeList.contains(strSubject)) {
				straNodeList.add(strSubject);
				if (strNodes.length() != 0)
					strNodes += "," + System.lineSeparator();
				strNodes += "{\"id\": \"" + strSubject + "\",";
				strNodes += "\"name\": \"" + strSubject + "\",";
				strNodes += "\"label\": \"" + strSubject + "\",";
				strNodes += "\"group\": \"Team A\",";
				strNodes += "\"runtime\": 20, \"category\":0}";
			}
			if (!straNodeList.contains(strObject)) {
				straNodeList.add(strObject);
				if (strNodes.length() != 0)
					strNodes += "," + System.lineSeparator();
				strNodes += "{\"id\": \"" + strObject + "\",";
				strNodes += "\"name\": \"" + strObject + "\",";
				strNodes += "\"label\": \"" + strObject + "\",";
				strNodes += "\"group\": \"Team A\",";
				strNodes += "\"runtime\": 20, \"category\":0}";
			}

			if (strLinks.length() != 0)
				strLinks += "," + System.lineSeparator();
			strLinks += "{\"source\": \"" + strSubject + "\",";
			strLinks += "\"target\": \"" + strObject + "\",";
			strLinks += "\"type\": \"" + strPredicate + "\"}";
		}
		if (strNodes.length() > 0)
			strNodes = "\"nodes\": [" + System.lineSeparator() + strNodes + System.lineSeparator() + "],"
					+ System.lineSeparator();
		if (strLinks.length() > 0)
			strLinks = "\"links\": [" + System.lineSeparator() + strLinks + System.lineSeparator() + "]"
					+ System.lineSeparator();

		/*
		 * { "nodes": [ {"id": "a", "name": "AGGR", "label": "Aggregation", "group":
		 * "Team C", "runtime": 20, "category":2}, {"id": "b", "name": "ASMT", "label":
		 * "Assessment Repository", "group": "Team A", "runtime":10} ], "links": [
		 * {"source": "a", "target": "b", "type": "Next -->>"}, {"source": "b",
		 * "target": "a", "type": "Next -->>"} ] }
		 */

		return "{" + System.lineSeparator() + strNodes + strLinks + "}";
	}
	
	public String getForceDirectedGraphModeledAndDerivedClasses() {

		List<String> straNodeList = new ArrayList<String>();
		String strSubject = "", strComposite = "", strLevel = "", strNodes = "", strLinks = "";

		ArrayList<HashMap<String, String>> list = executeQuery("""
																select ?s ?c ?l
																where {
																  ?s ?p ?o.
																  optional { ?s voc:subContextOf ?c.}
																  ?s voc:level ?l
																  Filter (?o in (voc:ModeledClass, voc:InducedClass, voc:DomainObject))  
																} ORDER BY ?l ?s
																""");

		for (int i = 0; i < list.size(); i++) {
			HashMap<?, ?> map = (HashMap<?, ?>) list.get(i);

			strSubject = map.get("s").toString();
			strComposite = map.get("c").toString();
			strLevel = map.get("l").toString();
			if (strLevel.indexOf("^^") >= 0)
				strLevel = strLevel.substring(0, strLevel.indexOf("^^"));

			if (!straNodeList.contains(strSubject)) {
				straNodeList.add(strSubject);
				if (strNodes.length() != 0)
					strNodes += "," + System.lineSeparator();
				strNodes += "{\"id\": \"" + strSubject + "\",";				
				strNodes += "\"group\": \"" + strLevel + "\",";
				strNodes += "\"name\": \"" + strSubject + "\",";
				strNodes += "\"label\": \"" + strSubject + "\",";
				strNodes += "\"runtime\": 20, \"category\":0}";
			}
			if (strSubject.length() > 0 && strComposite.length() > 0) {
				if (strLinks.length() != 0)
					strLinks += "," + System.lineSeparator();
				strLinks += "{\"source\": \"" + strSubject + "\",";
				strLinks += "\"target\": \"" + strComposite + "\",";
				strLinks += "\"type\": \"subContextOf\"}";
			}
		}
		
		list = executeQuery("""
				select *
				where {
				  ?s voc:partOf ?c.
				  Filter NOT EXISTS {?s rdfs:subClassOf ?x}
				} 
				""");
		for (int i = 0; i < list.size(); i++) {
			HashMap<?, ?> map = (HashMap<?, ?>) list.get(i);

			strSubject = map.get("s").toString();
			strComposite = map.get("c").toString();
			
			if (strSubject.length() > 0 && strComposite.length() > 0) {
				if (strLinks.length() != 0)
					strLinks += "," + System.lineSeparator();
				strLinks += "{\"source\": \"" + strSubject + "\",";
				strLinks += "\"target\": \"" + strComposite + "\",";
				strLinks += "\"type\": \"partOf\"}";
			}
		}
		
		if (strNodes.length() > 0)
			strNodes = "\"nodes\": [" + System.lineSeparator() + strNodes + System.lineSeparator() + "],"
					+ System.lineSeparator();
		if (strLinks.length() > 0)
			strLinks = "\"links\": [" + System.lineSeparator() + strLinks + System.lineSeparator() + "]"
					+ System.lineSeparator();

		/*
		 * { "nodes": [ {"id": "a", "name": "AGGR", "label": "Aggregation", "group":
		 * "Team C", "runtime": 20, "category":2}, {"id": "b", "name": "ASMT", "label":
		 * "Assessment Repository", "group": "Team A", "runtime":10} ], "links": [
		 * {"source": "a", "target": "b", "type": "Next -->>"}, {"source": "b",
		 * "target": "a", "type": "Next -->>"} ] }
		 */

		return "{" + System.lineSeparator() + strNodes + strLinks + "}";
	}

}
