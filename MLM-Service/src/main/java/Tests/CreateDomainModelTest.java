package Tests;


import org.apache.jena.rdf.model.Resource;
import MLMCore.DomainModel;


public class CreateDomainModelTest {
	
	public static void main(String[] args) {
		
		DomainModel dms = new DomainModel("myModel");
		dms.clearModel();
		
//		//example 1
//		Resource study = dms.modelClass("Study");
//		Resource experimentalStudyDesign = dms.modelAbstarctClass("ExperimentalStudyDesign", study);
//		Resource mrt = dms.modelConcreteClass("MicroRandomizedTrial", experimentalStudyDesign);
//		Resource rct = dms.modelConcreteClass("RandomizedControlledTrial", experimentalStudyDesign);
//		
//        Resource participant = dms.modelClass("Participant", study);
//        Resource affectedParticipant = dms.modelAbstarctClass("AffectedParticipant", participant);
//        Resource overweightParticipant = dms.modelConcreteClass("OverweightParticipant", affectedParticipant);
//        Resource participantWithHeartDisease = dms.modelConcreteClass("ParticipantWithHeartDisease", affectedParticipant);
//        Resource participantWithDiabetes = dms.modelConcreteClass("ParticipantWithDiabetes", affectedParticipant);
//        Resource healthyParticipant = dms.modelConcreteClass("HealthyParticipant", participant);
//        
//        Resource stateOfParticipant = dms.modelOccurrenceClass("StateOfParticipant", participant);
//        Resource phase1 = dms.modelConcreteClass("Phase1", stateOfParticipant);
//        Resource phase2 = dms.modelConcreteClass("Phase2", stateOfParticipant);
//        
//        Resource heartStudy = dms.createObject("HeartStudy", mrt);
//        Resource janeInHeartStudy = dms.createObject("JaneInHeartStudy", overweightParticipant, heartStudy);
//        
//        dms.createUse(mrt, healthyParticipant);
//        dms.createUse(participant, phase1);
		
		// example 2
		Resource study = dms.modelClass("Study");
		Resource mrt = dms.modelConcreteClass("MicroRandomizedTrial", study);

		Resource participant = dms.modelClass("Participant", study);
		Resource studentParticipant = dms.modelConcreteClass("StudentParticipant", participant);
		Resource femaleParticipant = dms.modelConcreteClass("FemaleParticipant", participant);

		dms.createUse(mrt, femaleParticipant);

        dms.propagateModel();
        dms.print();
        dms.writeForceDirectedGraphData2File("graph.json");
        dms.printInducedClasses();
        dms.write2File();
//        dms.writeForceDirectedGraphData2File("/Volumes/GoogleDrive/Meine Ablage/2021_Masterarbeit_JITAIs/D3/observable/graph.json");
//        dms.write2File();
        

        /*int someNumber = 42;
        String someString = "foobar";
        Object[] a = {someNumber, someString};
        MessageFormat fmt = new MessageFormat("String is \"{1}\", number is {0}.");
        System.out.println(fmt.format(a));*/
	}
}
