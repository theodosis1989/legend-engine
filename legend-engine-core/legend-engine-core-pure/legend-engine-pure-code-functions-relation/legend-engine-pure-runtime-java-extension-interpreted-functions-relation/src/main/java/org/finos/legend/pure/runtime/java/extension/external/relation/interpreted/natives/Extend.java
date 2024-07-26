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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.AggregationShared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Stack;

public class Extend extends AggregationShared
{
    public Extend(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        try
        {
            CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, processorSupport);

            TestTDS tds = getTDS(params, 0, processorSupport);

            RelationType<?> relationType = getRelationType(params, 0);
            GenericType sourceRelationType = (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType");

            CoreInstance secondParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

            TestTDS result;
            if (secondParameter instanceof FuncColSpec)
            {
                result = tds.addColumn(processFuncColSpec(tds.wrapFullTDS(), (FuncColSpec<?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType"), false));
            }
            else if (secondParameter instanceof FuncColSpecArray)
            {
                result = ((FuncColSpecArray<?, ?>) secondParameter)._funcSpecs().injectInto(
                        tds,
                        (a, funcColSpec) -> a.addColumn(processFuncColSpec(tds.wrapFullTDS(), funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, false))
                );
            }
            else if (secondParameter instanceof AggColSpec)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = tds.addColumn(processOneAggColSpec(source, (AggColSpec<?, ?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null));
            }
            else if (secondParameter instanceof AggColSpecArray)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = ((AggColSpecArray<?, ?, ?>) secondParameter)._aggSpecs().injectInto(
                        source.getOne(),
                        (a, aggColSpec) -> a.addColumn(processOneAggColSpec(source, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null))
                );
            }
            else if (Instance.instanceOf(secondParameter, "meta::pure::functions::relation::_Window", processorSupport))
            {
                MutableList<String> partitionIds = secondParameter.getValueForMetaPropertyToMany("partition").collect(PrimitiveUtilities::getStringValue).toList();
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = partitionIds.isEmpty() ? tds.wrapFullTDS() : tds.sort(partitionIds.collect(c -> new SortInfo(c, SortDirection.ASC)));

                ListIterable<? extends CoreInstance> sortInfos = secondParameter.getValueForMetaPropertyToMany("sortInfo");
                final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(Sort.getSortInfos(sortInfos, processorSupport).toList(), source);

                CoreInstance thirdParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
                if (thirdParameter instanceof AggColSpec)
                {
                    result = sortedPartitions.getOne().addColumn(processOneAggColSpec(sortedPartitions, (AggColSpec<?, ?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType));
                }
                else  if (thirdParameter instanceof AggColSpecArray)
                {
                    result = ((AggColSpecArray<?, ?, ?>) thirdParameter)._aggSpecs().injectInto(
                            sortedPartitions.getOne(),
                            (a, aggColSpec) -> a.addColumn(processOneAggColSpec(sortedPartitions, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType))
                    );
                }
                else if (thirdParameter instanceof FuncColSpec)
                {
                    result = sortedPartitions.getOne().addColumn(processFuncColSpec(sortedPartitions, (FuncColSpec<?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true));
                }
                else if (thirdParameter instanceof FuncColSpecArray)
                {
                    result = ((FuncColSpecArray<?, ?>) thirdParameter)._funcSpecs().injectInto(
                            sortedPartitions.getOne(),
                            (a, funcColSpec) -> a.addColumn(processFuncColSpec(sortedPartitions, funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true))
                    );
                }
                else
                {
                    throw new RuntimeException("Not possible");
                }
            }
            else
            {
                throw new RuntimeException("Not possible");
            }
            return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    private ColumnValue processFuncColSpec(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, FuncColSpec<?, ?> funcColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, boolean twoParamsFunc)
    {
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(funcColSpec.getValueForMetaPropertyToOne(M3Properties.function));
        String name = funcColSpec.getValueForMetaPropertyToOne(M3Properties.name).getName();

        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, lambdaFunction);

        Type type = ((FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[(int) source.getOne().getRowCount()];
            processOneColumn(source, lambdaFunction, (j, val) -> finalRes[j] = val == null ? null : PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.STRING, finalRes);
        }
        else if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.INT, finalRes, nulls);
        }
        else if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] finalRes = new double[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.DOUBLE, finalRes, nulls);
        }
        else
        {
            throw new RuntimeException("The type " + type._name() + " is not supported yet!");
        }
    }

    private interface Proc
    {
        void invoke();
    }

    private void processWithNull(Integer j, CoreInstance val, boolean[] nulls, Proc p)
    {
        {
            if (val == null)
            {
                nulls[j] = true;
            }
            else
            {
                p.invoke();
            }
        }
    }

    private void processOneColumn(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, LambdaFunction<CoreInstance> lambdaFunction, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, VariableContext evalVarContext, boolean twoParamsFunc)
    {
        FixedSizeList<CoreInstance> parameters = twoParamsFunc ? Lists.fixedSize.with((CoreInstance) null, (CoreInstance) null) : Lists.fixedSize.with((CoreInstance) null);
        int k = 0;
        for (int j = 0; j < source.getTwo().size(); j++)
        {
            Pair<Integer, Integer> r = source.getTwo().get(j);
            TestTDS sourceTDS = source.getOne().slice(r.getOne(), r.getTwo());
            for (int i = 0; i < r.getTwo() - r.getOne(); i++)
            {
                if (twoParamsFunc)
                {
                    parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(sourceTDS, relationType, repository, processorSupport), false, processorSupport));
                }
                parameters.set(twoParamsFunc ? 1 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(source.getOne(), i, "", null, relationType, -1, repository, false), false, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                setter.value(k++, newValue.getValueForMetaPropertyToOne("values"));
            }
        }
    }
}
