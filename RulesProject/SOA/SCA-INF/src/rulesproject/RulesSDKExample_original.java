package rulesproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.List;

import oracle.rules.sdk2.datamodel.BucketSetTable;
import oracle.rules.sdk2.datamodel.DataModel;
import oracle.rules.sdk2.datamodel.FactType;
import oracle.rules.sdk2.datamodel.Property;
import oracle.rules.sdk2.decisionpoint.DecisionPointDictionaryFinder;
import oracle.rules.sdk2.decisiontable.Bucket;
import oracle.rules.sdk2.decisiontable.BucketSet;
import oracle.rules.sdk2.decisiontable.DTAction;
import oracle.rules.sdk2.decisiontable.DTRule;
import oracle.rules.sdk2.decisiontable.Dimension;
import oracle.rules.sdk2.decisiontable.RuleSheet;
import oracle.rules.sdk2.dictionary.RuleDictionary;
import oracle.rules.sdk2.dictionary.UndoableEdit;
import oracle.rules.sdk2.exception.ConcurrentUpdateException;
import oracle.rules.sdk2.exception.SDKException;
import oracle.rules.sdk2.exception.SDKWarning;
import oracle.rules.sdk2.ruleset.Action;
import oracle.rules.sdk2.ruleset.Expression;
import oracle.rules.sdk2.ruleset.Pattern;
import oracle.rules.sdk2.ruleset.Rule;
import oracle.rules.sdk2.ruleset.RuleSet;
import oracle.rules.sdk2.ruleset.RuleSheetTable;
import oracle.rules.sdk2.ruleset.SimpleTest;


/*
 * This class demonstrates how to create and manipulate an Oracle Business Rule Dictionary
 * using the Rules API
 *
 * Specifically, how to
 *
 *   - Load an existing Rules Dictionary from the filesystem.
 *   - Add a Java fact to the dictionary Data Model.
 *   - Add a new BucketSet to the Dictionary Data Model.
 *   - Create and add a new Rule Set to the dictionary.
 *   - Associate the new Bucketset with a given Fact attribute.
 *   - Create a new If / Then Rule
 *   - Create a new Decision Table with multiple Rules that leverage the new Bucketset.
 *   - Validate and Save the updated Rules Dictionary
 *
 *   The class is packaged as a single file to simplify distribution.
 *   It has a dependency on one more source file ItemT.java, a java bean that is used as a java fact.
 *
 *   The class also has a dependency on a number of Oracle libraries.
 *   The following libraries need to be added under Project Settings to the SOA Project in Jdeveloper
 *   - Oracle Rules
 *   - Oracle XML Parser V2
 *   - ADF Model Runtime
 *   - Oracle JDBC
 *
 *
 *   Run the example by executing the Java Main of RulesSDKExample.java from within JDeveloper.
 *
 *   A constant is defined in the file that specify the Dictionary Name and location
 *       final String dictionaryLocation = "./oracle/rules/rulesexampleproject/MyRulesDictionary.rules";
 *
 *  For complete setup instructions, see the following blog post.
 *
 * @Author Bob Webster Feb 2013
 */

public class RulesSDKExample_original {

    public RulesSDKExample_original() {
        super();
    }

    /**
    * Demonstrates how to add a java fact to a dictionary data model
    * @param dictionary      An existing dictionary
    * @param javaclass       A java bean class
    */
    public static RuleDictionary  addFactsToDictionary(RuleDictionary dictionary, Class bean) throws SDKException {

        DataModel dataModel = dictionary.getDataModel();
        dataModel.addJavaClass(bean, false, null, null, null, null);
        return dictionary;

    }

    /* This method demonstrates how to add a new BucketSet to a dictionary data model
     *
     * @param   dict             An existing rules dictionary object
    */
    public static BucketSet addBucketSet(RuleDictionary dictionary)  throws Exception{

        DataModel model = dictionary.getDataModel();

        // Create a new bucketset holding product weight descriptions
        BucketSetTable bucketSetTable = model.getBucketSetTable();

        BucketSet productWeightBucketSet = bucketSetTable.getByName("ShipWeightType");

        if(productWeightBucketSet != null) {

            System.out.println("ShipWeightType BucketSet already exists, skipping add");
        }
        else {

            productWeightBucketSet = bucketSetTable.add();
            productWeightBucketSet.setName("ShipWeightType");
            productWeightBucketSet.setForm(BucketSet.FORM_LOV);
            productWeightBucketSet.add("Light");
            productWeightBucketSet.add("Heavy");
            productWeightBucketSet.add("VeryHeavy");

        }
        return productWeightBucketSet;
    }


    /* This method demonstrates how to associate a BucketSet to a dictionary fact attribute
     *
     * @param   bucketset    An existing bucketset
     * @param   factAlias    The alias name of an existing fact
     * @param   factProperty The name of a property contained in the fact
     * @param   model        An existing dictionary data model
     */
    public static void associateBucketSetToFact(BucketSet bucketset, String factAlias, String factProperty, DataModel model) {

            // Associate the BucketSet with a prticular fact
            FactType fType = model.getFactTypeTable().getByAlias(factAlias);

            if(fType == null)
                System.out.println("Cannot locate fact type ItemT");

            else {

                Property prop  = fType.getPropertyTable().getByName(factProperty);

                if(prop == null)
                    System.out.println("Cannot locate fact " + factAlias + " property " + factProperty);

                else
                   prop.setBucketSet(bucketset);
            }

    }


    /*
     * Demonstrates how to add a If-Then style rule to a ruleset
     * @param ruleset    an existing RuleSet
     */
    public static void addNewRuleToRuleset(RuleSet ruleset) {

        Rule shipSurchargeRule = ruleset.getRuleByName("ShipSurchargeRule");
        if(shipSurchargeRule != null) {

            System.out.println("shipSurchargeRule already exists, skipping add");
        }
        else {

            shipSurchargeRule = ruleset.getRuleTable().add();
            shipSurchargeRule.setName("ShipSurchargeRule");

            //add pattern to the rule
            Pattern p = shipSurchargeRule.getPatternTable().add();
            p.setForm(Pattern.FORM_FACT_TYPE);

            SimpleTest simple = p.getSimpleTestTable().add();

            simple.getLeft().setValue("ItemT.productWeight");
            simple.setOperator("==");

            // if using literal
            simple.getRight().setLiteralValue("VeryHeavy");

            // Add the Action to the Rule

            Action act = shipSurchargeRule.getActionTable().add();
            act.setForm(Action.FORM_MODIFY);
            act.setTarget("ItemT");

            // The form type of Action will determine what properties need to be set

            Expression extraCharge = act.getExpressionByParameterAlias("extraCharge");
            extraCharge.setValue("true");

        }
    }


    /* This method demonstrates how to add a Decision Table to a Rule Set
     *
     * @param   dict             An existing rules dictionary object
     * @param   shipBucketSet    An existing bucketset
     * @param   ruleset          An existing ruleset contained in the dictionary
     */
    public static void addDecisiontTableRuleToRuleset(RuleDictionary dict, BucketSet shipBucketSet, RuleSet ruleset) throws Exception {


        RuleSheetTable sheetTable = ruleset.getRuleSheetTable();
        RuleSheet dt = sheetTable.getByName("DecisionTable1");

        if(dt != null) {

            System.out.println("shipSurchargeRule already exists, skipping add");
        }
        else {

            // Add a new Rulesheet aka DecisionTable

            dt = ruleset.getRuleSheetTable().add();
            dt.setName("DecisionTable1");

            //
            // Add two dimensions aka condition rows to table
            //

            Dimension[] conditions = new Dimension[2];

            // First Row

            conditions[0] = dt.getDimensionTable().add();
            conditions[0].setValue("ItemT.productWeight");

            // Each condition must have an associated bucket set
            // In this case an existing named bucketset
            conditions[0].setSharedBucketSet(shipBucketSet);

            // Second Row

            conditions[1] = dt.getDimensionTable().add();
            conditions[1].setValue("ItemT.unitPrice.longValue()");

            // Auto create a Local bucket set for the second condition
            BucketSet bs = conditions[1].getBucketSet();
            bs.setForm(BucketSet.FORM_RANGE);
            Bucket buck = bs.add("<50");

            System.out.println("RuleTable size is " + dt.getDTRuleTable().size());

            // Add an Action for the decision table
            // A DTAction is an action that may be selected by any rule in the decision table,
            // optionally with different parameters per rule.
            // An ExpressionTable entry is created when an Action Form property is set so that it has the correct
            // number and type of parameters. ExpressionTable Rows cannot be directly added, deleted, or moved.

            DTAction dtAction = dt.getDTActionTable().add();
            dtAction.setForm(Action.FORM_MODIFY);
            dtAction.setTarget("ItemT");


            // Set the target expression as a decision table action parameter
             Expression surchargeExpr = dtAction.getExpressionByParameterAlias("shipSurcharge");

            // If each rule wants to set a different action value then the DTAction value shoudl be parameterized,
            // then each rule DTActionNode receives a dynamic copy of the expression (parameter table)
            surchargeExpr.setDTActionParameterName("shipSurcharge");

            // set a value for all rules, or a default value if parameterized
            surchargeExpr.setValue("10.0");


            // An initial default rule is added to the Decision Table when a dimension is added to the decision table
            // The default rule has no dimensions set.
            // If we remove the initial rule, it also removes all dimensions from the DimensionTable
            // The default rule must have its dimensions set before canAdd() will be true and more rules can be added to the rule table
            // The canAdd() method on the RuleTable indicates whether a new rule can be added
            // New rules can only be added to the rule table if all the existing rules in the  table have dimensions specified.
            // Also if two rules are added with identical condition values, the second add is ignored

            System.out.println("Rules can be added? " + dt.getDTRuleTable().canAdd());
            DTRule dtRuleDef = dt.getDTRuleTable().get(0);

            dtRuleDef.getDimensionNode(0).setValues("otherwise");
            dtRuleDef.getDimensionNode(1).setValues(">=50");

            if(!updateRuleDictionary(dict))
                System.out.println("UNABLE to update dictionary.");
            else
                System.out.println("Updated dictionary");

            // Next add more rules to the table, 1 for each value in the ship bucket set productWeight

            BucketSet bucketSet = conditions[0].getBucketSet();
            List<Bucket> bcks = bucketSet.getBuckets();

            for(int i =1 ; i<bcks.size() - 1 ; i++) // skip the first bucket value handled by the default rule
            {
               System.out.println("Rules can be added " + dt.getDTRuleTable().canAdd());
               DTRule dtRule = dt.getDTRuleTable().add();   // use returned rule, cannot use rule returned by .get(i) ?
               System.out.println("Added new rule");

               // Set the dimension/condition values for the new rule
               dtRule.getDimensionNode(0).setValues(bcks.get(i).getName());  // "Heavy, Very Light etc"
               dtRule.getDimensionNode(1).setValues(">=50");

              // An update is required here so a DTActionNode Expression table entry for the new rule will be created.
              // If an expression parameter in the DTAction exists, then the expression is copied from the DTActionNode of the new Rule.
              // A value for the parameter will be added later after the update

               if(!updateRuleDictionary(dict))
                   System.out.println("UNABLE to update dictionary.");
               else
                   System.out.println("Updated dictionary");
           }


            // Loop again and set the parmameter values for each rule expression
            for(int i =1 ; i<bcks.size() - 1 ; i++) // skip the first bucket value handled by the default rule
            {
                DTRule dtRuleDef1 = dt.getDTRuleTable().get(i);

                Expression expr =  dtRuleDef1.getDTActionNode(0).getExpressionByDTActionParameterName("shipSurcharge");
                // Estimate a cost
                if(expr !=null)
                   expr.setValue(Double.toString(i * 15.00));
            }

            System.out.println("Rule table now contains " + dt.getDTRuleTable().size() + " rules");

            if(!updateRuleDictionary(dict))
                System.out.println("UNABLE to update dictionary.");
            else
                System.out.println("Updated dictionary");

        }

    }



    /**
     * Loads the rule dictionary from the specified dictionaryPath
     * @param dictionaryLocation The full path to the .rules file.
     * @return A rule dictionary object
     * @see readDictionary http://docs.oracle.com/cd/E23943_01/apirefs.1111/e10663/toc.htm
     *
     */
    public static RuleDictionary loadRuleDictionary(String dictionaryLocation) throws Exception{
            RuleDictionary dict = null;
            Reader reader = null;
            Writer writer = null;

            try {
                reader = new FileReader(new File(dictionaryLocation));
                dict = RuleDictionary.readDictionary(reader, new DecisionPointDictionaryFinder(null));
                List<SDKWarning> warnings = new ArrayList<SDKWarning>();

                dict.update(warnings);
                if (warnings.size() > 0 ) {
                    System.err.println("Validation warnings: " + warnings);
                }

            } finally {
                if (reader != null) { try { reader.close(); }
                                      catch (IOException ioe) {ioe.printStackTrace();}}
                if (writer != null) { try { writer.close(); }
                                      catch (IOException ioe) {ioe.printStackTrace();}}
            }

            return dict;
        }

    /**
    * Update the rule dictionary from the specified dictionaryPath
    * @param A rule dictionary object
    * @return boolean true if the update was successful otherwise false.
    * @see See writeDictionary http://docs.oracle.com/cd/E23943_01/apirefs.1111/e10663/toc.htm
    *
    */
    public static boolean updateRuleDictionary(RuleDictionary dictionary) throws Exception{


      UndoableEdit undo = null;
      List<SDKWarning> warnings = new ArrayList<SDKWarning>();
      boolean rc = false;

      try {
           undo = dictionary.update(warnings);
           rc = true;
        }
        catch (ConcurrentUpdateException e) {
           dictionary.rollback();
        }
        catch (SDKException e) {
           dictionary.rollback();
        }
      return rc;

    }

    /**
    * Stores the rule dictionary from the specified dictionaryPath
    * @param A rule dictionary object
    * @param dictionaryLocation The full path to the .rules file.
    * @see See writeDictionary http://docs.oracle.com/cd/E23943_01/apirefs.1111/e10663/toc.htm
    *
    */
    public static void storeRuleDictionary(RuleDictionary dictionary, String dictionaryLocation) throws Exception{

        List<SDKWarning> warnings = new ArrayList<SDKWarning>();
        List<SDKException> errors = new ArrayList<SDKException>();

        dictionary.validate(errors, warnings);

        if (warnings.size() > 0 ) {
            System.err.println("Validation warnings: " + warnings);
        }

        if(errors.size() > 0) {

            System.err.println("Validation errors: " + errors);
            System.out.println("Skipping write of rule dictionary");

        } else
        {

            StringWriter swriter = new StringWriter();
            dictionary.writeDictionary(swriter);
            Writer writer = null;
            try {
                    writer = new OutputStreamWriter(new FileOutputStream(new File(dictionaryLocation)),"UTF-8");
                    writer.write(swriter.toString());
                } finally {
                    if (writer != null) try {
                                            writer.close();
                                         } catch (IOException e)
                                            {
                                               System.out.println("Warning: Unable to close dictionary writer.");
                                            }
                  }

        }

      }

    /**
    * Creates a new Rules Dictionary in memory
    * @param dictionaryName      A name for the dictionary, used as a className
    * @param dictionaryPackage   A name for the dictionary package, used as a java package name.
    */
    public static RuleDictionary  createDictionaryInMemory(String dictionaryName, String dictionaryPackage) throws SDKException {

        RuleDictionary dictionary = RuleDictionary.createDictionary(dictionaryName, new DecisionPointDictionaryFinder());
        dictionary.setName(dictionaryName);
        dictionary.setPackage(dictionaryPackage);

        return dictionary;

    }






    public static void main(String args[]) throws Exception {

        // location for an existing dictionary
        final String dictionaryLocation = "E:\\JDeveloperWorkspace\\RulesSDKApplication_pradipta\\RulesProject\\SOA\\oracle\\rules\\rulesproject\\MyRulesDictionary.rules";

        // Load an existing dictionary, create a session handle in case JDeveloper or some other
        // user is also editing the dictionary.

        RuleDictionary  dictionary = loadRuleDictionary(dictionaryLocation).createHandle();

        // Delete the existing model, in case we ran previously
        dictionary.getDataModel().clear();

        // Add a new Java Fact to the dictionary
        dictionary = addFactsToDictionary(dictionary, rulesproject.ItemT.class);

        // Add a new bucketset to the dictionary
        BucketSet licenseBucketSet = addBucketSet(dictionary);

        // Associate the new BucketSet with new Fact field
        associateBucketSetToFact(licenseBucketSet, "ItemT", "productWeight", dictionary.getDataModel());

        // update dictionary
        if(!updateRuleDictionary(dictionary))
            System.out.println("UNABLE to update dictionary.");


        // check for existing ruleset named MyRuleSet
        RuleSet myRuleSet = dictionary.getRuleSet("MyRuleSet");

        // if it does exist, because we ran before, remove it
        if(myRuleSet != null)
        {
            dictionary.removeRuleSet("MyRuleSet") ;
            if(!updateRuleDictionary(dictionary))
                System.out.println("UNABLE to update dictionary.");
            System.out.println("Removed old ruleset");
        }

        // Add a new Ruleset
        myRuleSet = dictionary.createEmptyRuleSet("MyRuleSet");
        if(!updateRuleDictionary(dictionary))
                System.out.println("UNABLE to update dictionary.");

        // Add a new 'if-then' rule to the RuleSet
        addNewRuleToRuleset(myRuleSet);
        if(!updateRuleDictionary(dictionary))
            System.out.println("UNABLE to update dictionary.");


        // Add a decisionTable with rules that use the Bucketset
        addDecisiontTableRuleToRuleset(dictionary, licenseBucketSet, myRuleSet);

        // Update and rewrite the dictionary file

        boolean success = updateRuleDictionary(dictionary);
        if(success)
        {
                storeRuleDictionary(dictionary, dictionaryLocation);
                System.out.println("Wrote dictionary to filesystem");
        }
        else
            System.out.println("Unable to update dictionary");

        }
}
