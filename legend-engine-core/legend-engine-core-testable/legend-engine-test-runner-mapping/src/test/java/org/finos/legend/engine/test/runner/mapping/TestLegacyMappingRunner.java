// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.test.runner.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Test;

import java.io.IOException;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;
import static org.junit.Assert.assertEquals;

public class TestLegacyMappingRunner
{
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    private PlanExecutor planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();


    //Legacy mapping test runner tests

    private String dbAndModel = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PersonTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    firmId INTEGER,\n" +
            "    lastName VARCHAR(200)\n" +
            "  )\n" +
            "  Table FirmTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    legalName VARCHAR(200)\n" +
            "  )\n" +
            "\n" +
            "  Join FirmPerson(PersonTable.firmId = FirmTable.id)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class test::Firm\n" +
            "{\n" +
            "  employees: test::Person[*];\n" +
            "  legalName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "\n" +
            "\n";

    private String sharedMapping = "  *test::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]PersonTable.id\n" +
            "    )\n" +
            "    ~mainTable [test::DB]PersonTable\n" +
            "    lastName: [test::DB]PersonTable.lastName,\n" +
            "    firstName: [test::DB]PersonTable.lastName\n" +
            "  }\n" +
            "  *test::Firm: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]FirmTable.id\n" +
            "    )\n" +
            "    ~mainTable [test::DB]FirmTable\n" +
            "    employees[test_Person]: [test::DB]@FirmPerson,\n" +
            "    legalName: [test::DB]FirmTable.legalName\n" +
            "  }\n";


    @Test
    public void testRelationalSuccessfulTestSQLExecution() throws IOException
    {
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(
                dbAndModel +
                        "###Mapping\n" +
                        "Mapping test::MyMapping\n" +
                        "(\n" +
                        sharedMapping +
                        "  MappingTests\n" +
                        "  [\n" +
                        "    test_1\n" +
                        "    (\n" +
                        "      query: |test::Person.all()->project([p|$p.lastName],['lastName']);\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        <Relational, SQL, test::DB, 'Drop table if exists PersonTable;\\nCreate Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\nInsert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\nInsert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>\n" +
                        "      ];\n" +
                        "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Doe2\" ]\\n} ]';\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")\n");
        PureModel pureModel = new PureModel(pureModelContextData, Identity.getAnonymousIdentity().getName(), Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("test::MyMapping", testResult.getMappingPath());
        assertEquals("test_1", testResult.getTestName());
        assertEquals(org.finos.legend.engine.test.runner.shared.TestResult.SUCCESS, testResult.getResult());
    }

    @Test
    public void testRelationalSuccessfulTestCSVExecution() throws IOException
    {
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(
                dbAndModel +
                        "###Mapping\n" +
                        "Mapping test::MyMapping\n" +
                        "(\n" +
                        sharedMapping +
                        "  MappingTests\n" +
                        "  [\n" +
                        "    test_1\n" +
                        "    (\n" +
                        "      query: |test::Person.all()->project([p|$p.lastName],['lastName']);\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        <Relational, CSV, test::DB, 'default\\nPersonTable\\nid,lastName\\n1,Doe;\\n2,Doe2\\n\\n\\n\\n'>\n" +
                        "      ];\n" +
                        "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Doe2\" ]\\n} ]';\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")\n");
        PureModel pureModel = new PureModel(pureModelContextData, Identity.getAnonymousIdentity().getName(), Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("test::MyMapping", testResult.getMappingPath());
        assertEquals("test_1", testResult.getTestName());
        assertEquals(org.finos.legend.engine.test.runner.shared.TestResult.SUCCESS, testResult.getResult());
    }


    @Test
    public void testRelationalFailureTestSQLExecution() throws IOException
    {
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(
                dbAndModel +
                        "###Mapping\n" +
                        "Mapping test::MyMapping\n" +
                        "(\n" +
                        sharedMapping +
                        "  MappingTests\n" +
                        "  [\n" +
                        "    test_1\n" +
                        "    (\n" +
                        "      query: |test::Person.all()->project([p|$p.lastName],['lastName']);\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        <Relational, SQL, test::DB, 'Drop table if exists PersonTable;\\nCreate Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\nInsert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\nInsert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>\n" +
                        "      ];\n" +
                        "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Wrong\" ]\\n} ]';\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")\n");
        PureModel pureModel = new PureModel(pureModelContextData, Identity.getAnonymousIdentity().getName(), Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("test::MyMapping", testResult.getMappingPath());
        assertEquals("test_1", testResult.getTestName());
        assertEquals(org.finos.legend.engine.test.runner.shared.TestResult.FAILURE, testResult.getResult());
        assertEquals(
                objectMapper.readValue("[ {\n  \"values\" : [ \"Doe;\" ]\n}, {\n  \"values\" : [ \"Wrong\" ]\n} ]", JsonNode.class),
                objectMapper.readValue(testResult.getExpected().get(), JsonNode.class));
        assertEquals(
                objectMapper.readValue("[ {\n  \"values\" : [ \"Doe;\" ]\n}, {\n  \"values\" : [ \"Doe2\" ]\n} ]", JsonNode.class),
                objectMapper.readValue(testResult.getActual().get(), JsonNode.class));
    }

    @Test
    public void testRelationalFailureTestCSVExecution() throws IOException
    {
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(
                dbAndModel +
                        "###Mapping\n" +
                        "Mapping test::MyMapping\n" +
                        "(\n" +
                        sharedMapping +
                        "  MappingTests\n" +
                        "  [\n" +
                        "    test_1\n" +
                        "    (\n" +
                        "      query: |test::Person.all()->project([p|$p.lastName],['lastName']);\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        <Relational, CSV, test::DB, 'default\\nPersonTable\\nid,lastName\\n1,Doe;\\n2,Doe2\\n\\n\\n\\n'>\n" +
                        "      ];\n" +
                        "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Wrong\" ]\\n} ]';\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")\n");
        PureModel pureModel = new PureModel(pureModelContextData, Identity.getAnonymousIdentity().getName(), Thread.currentThread().getContextClassLoader(), DeploymentMode.PROD);

        RichMappingTestResult testResult = runTest(pureModelContextData, pureModel);

        assertEquals("test::MyMapping", testResult.getMappingPath());
        assertEquals("test_1", testResult.getTestName());
        assertEquals(org.finos.legend.engine.test.runner.shared.TestResult.FAILURE, testResult.getResult());
        assertEquals(
                objectMapper.readValue("[ {\n  \"values\" : [ \"Doe;\" ]\n}, {\n  \"values\" : [ \"Wrong\" ]\n} ]", JsonNode.class),
                objectMapper.readValue(testResult.getExpected().get(), JsonNode.class));
        assertEquals(
                objectMapper.readValue("[ {\n  \"values\" : [ \"Doe;\" ]\n}, {\n  \"values\" : [ \"Doe2\" ]\n} ]", JsonNode.class),
                objectMapper.readValue(testResult.getActual().get(), JsonNode.class));
    }

    private RichMappingTestResult runTest(PureModelContextData pureModelContextData, PureModel pureModel)
    {
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = pureModelContextData.getElementsOfType(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class).get(0);
        MappingTest_Legacy mappingTestLegacy = mapping.tests.get(0);
        MappingTestRunner mappingTestRunner = new MappingTestRunner(pureModel, mapping.getPath(), mappingTestLegacy, planExecutor, Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, "vX_X_X");
        return mappingTestRunner.setupAndRunTest();
    }


}
