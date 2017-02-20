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

import oracle.rules.sdk2.decisionpoint.DecisionPointDictionaryFinder;
import oracle.rules.sdk2.decisiontable.Bucket;
import oracle.rules.sdk2.decisiontable.BucketSet;
import oracle.rules.sdk2.decisiontable.DTRule;
import oracle.rules.sdk2.decisiontable.DimensionNode;
import oracle.rules.sdk2.decisiontable.RuleSheet;
import oracle.rules.sdk2.dictionary.RuleDictionary;
import oracle.rules.sdk2.dictionary.UndoableEdit;
import oracle.rules.sdk2.exception.ConcurrentUpdateException;
import oracle.rules.sdk2.exception.SDKException;
import oracle.rules.sdk2.exception.SDKWarning;
import oracle.rules.sdk2.ruleset.Expression;
import oracle.rules.sdk2.ruleset.RuleSet;
import oracle.rules.sdk2.ruleset.RuleSheetTable;


public class RulesSDKExample_pradipta {

    public RulesSDKExample_pradipta() {
        super();
    }


    public static RuleDictionary loadRuleDictionary(String dictionaryLocation) throws Exception {
        RuleDictionary dict = null;
        Reader reader = null;
        Writer writer = null;

        try {
            reader = new FileReader(new File(dictionaryLocation));
            dict = RuleDictionary.readDictionary(reader, new DecisionPointDictionaryFinder(null));
            List<SDKWarning> warnings = new ArrayList<SDKWarning>();

            dict.update(warnings);
            if (warnings.size() > 0) {
                System.err.println("Validation warnings: " + warnings);
            }

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        return dict;
    }

    public static boolean updateRuleDictionary(RuleDictionary dictionary) throws Exception {
        UndoableEdit undo = null;
        List<SDKWarning> warnings = new ArrayList<SDKWarning>();
        boolean rc = false;

        try {
            undo = dictionary.update(warnings);
            rc = true;
        } catch (ConcurrentUpdateException e) {
            dictionary.rollback();
        } catch (SDKException e) {
            dictionary.rollback();
        }
        return rc;

    }

    public static void storeRuleDictionary(RuleDictionary dictionary, String dictionaryLocation) throws Exception {

        List<SDKWarning> warnings = new ArrayList<SDKWarning>();
        List<SDKException> errors = new ArrayList<SDKException>();

        dictionary.validate(errors, warnings);

        if (warnings.size() > 0) {
            System.err.println("Validation warnings: " + warnings);
        }

        if (errors.size() > 0) {

            System.err.println("Validation errors: " + errors);
            System.out.println("Skipping write of rule dictionary");

        } else {

            StringWriter swriter = new StringWriter();
            dictionary.writeDictionary(swriter);
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(new File(dictionaryLocation)), "UTF-8");
                writer.write(swriter.toString());
            } finally {
                if (writer != null)
                    try {
                        writer.close();
                    } catch (IOException e) {
                        System.out.println("Warning: Unable to close dictionary writer.");
                    }
            }

        }

    }

    public static String processBucketForDimensionNode(DimensionNode dNode, String value) throws SDKException {
        BucketSet buckSet = dNode.getBucketSet();
        List<Bucket> bcks = buckSet.getBuckets();
        int index = -1;
        String dimensionNodeValue = null;
        for (Bucket bck : bcks) {
            if ((bck.getName().equals("\"" + value + "\"")) || (bck.getName().equals(value))) {
                index = bcks.indexOf(bck);
            }
        }
        if (index != -1) {
            dimensionNodeValue = dNode.getBucketSet().getBuckets().get(index).getName();
            System.out.println("not null index values : "+dimensionNodeValue);
        } else {
            dNode.getBucketSet().add(value);
            try
            {
                int temp = Integer.parseInt(value);
                dimensionNodeValue = value;
            }
            catch(Exception e) {
                dimensionNodeValue = "\"" + value + "\"";
            }
            System.out.println("null index values : "+dimensionNodeValue);
        }
        
        return dimensionNodeValue;
    }


    public static void updateDecisiontTableRuleToRuleset(RuleSet ruleset, RuleDictionary dictionary) throws Exception {
        RuleSheetTable sheetTable = ruleset.getRuleSheetTable();
        RuleSheet dt = sheetTable.getByName("DecisionTable1");
        if (dt != null) {
            int dtRuleSize = dt.getDTRuleTable().size();
            System.out.println("Size of Decision table : " + dt.getDTRuleTable().size());
            for (int i = 0; i <= 5; i++) {
                DTRule dtRule = dt.getDTRuleTable().add(); // use returned rule, cannot use rule returned by .get(i) ?
//                System.out.println("Added rule : " + (dtRuleSize + i));
                dtRule.setDescription("Rule Number " + (dtRuleSize + i));

                //each dimensionNode is a condition, with 0 to n mapping to C1 to C(n+1)
                // - means dont care
                if ((i % 3 == 2)) {
                    dtRule.getDimensionNode(0).setValues("-");
                    dtRule.getDimensionNode(1).setValues("-");
                    dtRule.getDimensionNode(2).setValues("-");
                    dtRule.getDimensionNode(3).setValues("-");
                    dtRule.getDimensionNode(4).setValues("-");
                    dtRule.getDimensionNode(5).setValues("-");
                    dtRule.getDimensionNode(6).setValues("-");
                    dtRule.getDimensionNode(7).setValues("-");
                    
                    if(!updateRuleDictionary(dictionary))
                        System.out.println("UNABLE to update dictionary.");
                    else
                        System.out.println("Updated dictionary");
                    
                    DTRule dtRuleForAction = dt.getDTRuleTable().get(dtRuleSize + i);
                    Expression expr =
                                dtRuleForAction.getDTActionNode(0).getExpressionByDTActionParameterName("empTrained");
                    if (expr != null)
                        expr.setValue("\"YES\"");
                    dtRule.getDTActionNode(0).getActionSelectedProperty().setValue(true);
                
                } else if ((i % 3 == 1)) {
                    dtRule.getDimensionNode(0).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(0),
                                                                                       "aaa"));
                    dtRule.getDimensionNode(1).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(1),
                                                                                       "bbb"));
                    dtRule.getDimensionNode(2).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(2),
                                                                                       "ccc"));
                    dtRule.getDimensionNode(3).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(3),
                                                                                       Integer.toString(56 + i)));
                    dtRule.getDimensionNode(4).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(4),
                                                                                       "ddd"));
                    dtRule.getDimensionNode(5).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(5),
                                                                                       "eee"));
                    dtRule.getDimensionNode(6).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(6),
                                                                                       "fff"));
                    dtRule.getDimensionNode(7).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(7),
                                                                                       "ggg"));
                    if(!updateRuleDictionary(dictionary))
                        System.out.println("UNABLE to update dictionary.");
                    else
                        System.out.println("Updated dictionary");
                    
                    DTRule dtRuleForAction = dt.getDTRuleTable().get(dtRuleSize + i);
                    Expression expr =
                                dtRuleForAction.getDTActionNode(0).getExpressionByDTActionParameterName("empTrained");
                    if (expr != null)
                        expr.setValue("\"MAY BE\"");
                    dtRule.getDTActionNode(0).getActionSelectedProperty().setValue(true);
                    dtRule.getDTActionNode(0).getActionSelectedProperty().setValue(true);
                } else {
                    dtRule.getDimensionNode(0).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(0),
                                                                                       "zzz"));
                    dtRule.getDimensionNode(1).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(1),
                                                                                       "yyy"));
                    dtRule.getDimensionNode(2).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(2),
                                                                                       "xxx"));
                    dtRule.getDimensionNode(3).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(3),
                                                                                       Integer.toString(56 + i)));
                    dtRule.getDimensionNode(4).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(4),
                                                                                       "www"));
                    dtRule.getDimensionNode(5).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(5),
                                                                                       "vvv"));
                    dtRule.getDimensionNode(6).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(6),
                                                                                       "uuu"));
                    dtRule.getDimensionNode(7).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(7),
                                                                                       "ttt"));
                    if(!updateRuleDictionary(dictionary))
                        System.out.println("UNABLE to update dictionary.");
                    else
                        System.out.println("Updated dictionary");
                    
                    DTRule dtRuleForAction = dt.getDTRuleTable().get(dtRuleSize + i);
                    Expression expr =
                                dtRuleForAction.getDTActionNode(0).getExpressionByDTActionParameterName("empTrained");
                    if (expr != null)
                        expr.setValue("\"NO\"");
                    dtRule.getDTActionNode(0).getActionSelectedProperty().setValue(true);
                }
                /*
            * Below section can be used to create a new action table and control if it will be always selected by default or not.
            DTAction dtAction = dt.getDTActionTable().add();
            dtAction.setForm(Action.FORM_MODIFY);
            dtAction.setTarget("Employee");
            dtAction.getAlwaysSelectedProperty().setValue(false);
            System.out.println("Size of the action : " + dtAction.isAlwaysSelected());
            Expression surchargeExpr = dtAction.getExpressionByParameterAlias("empTrained");
            surchargeExpr.setDTActionParameterName("empTrained");
            surchargeExpr.setValue("\"NO\"");
*/
            }
        }
    }

    public static void main(String args[]) throws Exception {

        final String dictionaryLocation =
            "D:\\WorkSpace\\JDEV_1213_PoC\\RulesSDKApplication_pradipta\\RulesProject\\SOA\\oracle\\rules\\rulesproject\\MyRulesDecisionTableSimplest.rules";

        RuleDictionary dictionary = loadRuleDictionary(dictionaryLocation).createHandle();

        if (!updateRuleDictionary(dictionary))
            System.out.println("UNABLE to update dictionary.");

        RuleSet myRuleSet = dictionary.getRuleSet("Ruleset1");

        if (myRuleSet != null) {
            System.out.println("Rule Set exist");
        } else {
            System.out.println("Rule Set doesn't exist");
        }

        updateDecisiontTableRuleToRuleset(myRuleSet,dictionary);
        boolean success = updateRuleDictionary(dictionary);
        if (success) {
            storeRuleDictionary(dictionary, dictionaryLocation);
            System.out.println("Wrote dictionary to filesystem");
        } else
            System.out.println("Unable to update dictionary");
    }
}
