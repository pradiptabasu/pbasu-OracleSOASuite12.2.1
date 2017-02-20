package imonitrules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


//Map<String, String> map = new HashMap<String, String>();
//map.put("1", "Jan");
//map.put("2", "Feb");
//map.put("3", "Mar");
//

public class BusinessRulesAppendAPI {

    RuleDictionary dictionary;
    RuleSet myRuleSet;

    public BusinessRulesAppendAPI() {
        super();
    }

    public void setMyRuleSet(RuleSet myRuleSet) {
        this.myRuleSet = myRuleSet;
    }

    public RuleSet getMyRuleSet() {
        return myRuleSet;
    }

    public void setDictionary(RuleDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public RuleDictionary getDictionary() {
        return dictionary;
    }

    public RuleDictionary loadRuleDictionary(String dictionaryLocation) throws Exception {
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

    public boolean updateRuleDictionary() throws Exception {
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

    public void storeRuleDictionary(String dictionaryLocation) throws Exception {
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

    public String processBucketForDimensionNode(DimensionNode dNode, String value) throws SDKException {
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
            System.out.println("not null index values : " + dimensionNodeValue);
        } else {
            dNode.getBucketSet().add(value);
            try {
                int temp = Integer.parseInt(value);
                dimensionNodeValue = value;
            } catch (Exception e) {
                dimensionNodeValue = "\"" + value + "\"";
            }
            System.out.println("null index values : " + dimensionNodeValue);
        }
        return dimensionNodeValue;
    }

    public void updateDecisiontTableRuleToRuleset(RuleSet ruleset, String decisionRuleName,
                                                  ArrayList<String> conditionValueList,
                                                  Map<String, String> actionExpn) throws Exception {
        RuleSheetTable sheetTable = ruleset.getRuleSheetTable();
        RuleSheet dt = sheetTable.getByName(decisionRuleName);
        if (dt != null) {
            int dtRuleSize = dt.getDTRuleTable().size();
            System.out.println("Size of Decision table : " + dt.getDTRuleTable().size());
            DTRule dtRule = dt.getDTRuleTable().add();
            dtRule.setDescription("Rule Number " + (dtRuleSize + 1));
            for (int i = 0; i < conditionValueList.size(); i++) {
                dtRule.getDimensionNode(i).setValues(processBucketForDimensionNode(dtRule.getDimensionNode(i),
                                                                                   conditionValueList.get(i)));
            }
            if (updateRuleDictionary()) {
                DTRule dtRuleForAction = dt.getDTRuleTable().get(dtRuleSize + 1);
                Expression expr = null;
                for (Map.Entry<String, String> entry : actionExpn.entrySet()) {
                    expr = dtRuleForAction.getDTActionNode(0).getExpressionByDTActionParameterName(entry.getKey());
                    if (expr != null)
                        expr.setValue("\"" + entry.getValue() + "\"");
                }
                dtRule.getDTActionNode(0).getActionSelectedProperty().setValue(true);
            } else {
                System.out.println("UNABLE to update dictionary.");
            }
        }
    }


    public void processExcelRuleFile(String excelFilePath, String decisionRuleName,
                                     int conditionListSize) throws FileNotFoundException, IOException, Exception {
        ArrayList<String> conditionValueList = new ArrayList<String>();
        Map<String, String> actionExpn = new HashMap<String, String>();

        FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = firstSheet.iterator();

        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            conditionValueList.clear();
            actionExpn.clear();
            System.out.print("Row " + nextRow.getRowNum() + ", ");
            for (int i = 1; i <= conditionListSize; i++) {
                switch (i) {
                case 1: //Transaction$TransactionDetails.serviceProvider.toUpperCase()
                    conditionValueList.add(nextRow.getCell(7).getStringCellValue());
                    break;
                case 2: //Transaction$TransactionDetails.messageType.toUpperCase()
                    conditionValueList.add(nextRow.getCell(3).getStringCellValue());
                    break;
                case 3: //Transaction$TransactionDetails.direction.toUpperCase()
                    conditionValueList.add(nextRow.getCell(6).getStringCellValue());
                    break;
                case 4: //Transaction$TransactionDetails.source.toUpperCase()
                    conditionValueList.add(nextRow.getCell(4).getStringCellValue());
                    break;
                case 5: //Transaction$TransactionDetails.target.toUpperCase()
                    conditionValueList.add(nextRow.getCell(5).getStringCellValue());
                    break;
                case 6: //Transaction$TransactionDetails.currentTime
                    conditionValueList.add("aaa");
                    break;
                case 7: //Transaction$TransactionDetails.threshold
                    conditionValueList.add("bbb");
                    break;
                default:
                    break;
                }
            }
            updateDecisiontTableRuleToRuleset(getMyRuleSet(), decisionRuleName, conditionValueList, actionExpn);
        }
        System.out.println();

        workbook.close();
        inputStream.close();
    }

    public static void main(String args[]) throws Exception {
        String ruleSetName = "idleTimeAlert";
        String decisionRuleName = "checkIdleTimeAlert";
        int conditionListSize = 7;
        String dictionaryLocation =
            "D:\\WorkSpace\\JDEV_122100_PoC\\RulesSDKApplication_pradipta\\iMonitRules\\SOA\\oracle\\rules\\iMonitRules\\iMonitRules.rules";
        String excelFilePath = "D:\\WorkSpace\\JDEV_122100_PoC\\RulesSDKApplication_pradipta\\CONFIGL3_Data.xlsx";

        BusinessRulesAppendAPI businessRulesAppendApi = new BusinessRulesAppendAPI();

        businessRulesAppendApi.setDictionary(businessRulesAppendApi.loadRuleDictionary(dictionaryLocation).createHandle());

        if (businessRulesAppendApi.updateRuleDictionary()) {
            businessRulesAppendApi.setMyRuleSet(businessRulesAppendApi.getDictionary().getRuleSet(ruleSetName));
            if (businessRulesAppendApi.getMyRuleSet() != null) {
                businessRulesAppendApi.processExcelRuleFile(excelFilePath, decisionRuleName, conditionListSize);
                if (businessRulesAppendApi.updateRuleDictionary()) {
                    businessRulesAppendApi.storeRuleDictionary(dictionaryLocation);
                    System.out.println("Wrote dictionary to filesystem");
                } else
                    System.out.println("Unable to update dictionary");
            } else {
                System.out.println("Rule Set doesn't exist");
            }
        } else {
            System.out.println("UNABLE to update dictionary.");
        }
    }
}
