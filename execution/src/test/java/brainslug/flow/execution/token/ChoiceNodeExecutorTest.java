package brainslug.flow.execution.token;

import brainslug.AbstractExecutionTest;
import brainslug.flow.FlowBuilder;
import brainslug.flow.FlowDefinition;
import brainslug.flow.Identifier;
import brainslug.flow.context.*;
import brainslug.flow.execution.DefaultExecutionContext;
import brainslug.flow.execution.DefaultExecutionProperties;
import brainslug.flow.execution.FlowNodeExecutionResult;
import brainslug.flow.execution.FlowNodeExecutor;
import brainslug.flow.execution.expression.DefaultPredicateEvaluator;
import brainslug.flow.execution.expression.PredicateEvaluator;
import brainslug.flow.execution.expression.PropertyPredicate;
import brainslug.flow.node.ChoiceDefinition;
import brainslug.util.IdUtil;
import org.junit.Test;

import static brainslug.util.IdUtil.id;
import static brainslug.util.TestId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ChoiceNodeExecutorTest extends AbstractExecutionTest {

  @Test
  public void shouldExecuteChoiceTruePath() {
    // given:
    ChoiceNodeExecutor choiceNodeExecutor = createChoiceNodeExecutor();

    FlowDefinition flowDefinition = choiceFlow();
    Trigger trigger = new Trigger().property("foo", "bar");
    DefaultExecutionContext execution = new DefaultExecutionContext(trigger, registryWithServiceMock());

    // when:
    FlowNodeExecutionResult result = choiceNodeExecutor.execute(flowDefinition.getNode(id(CHOICE), ChoiceDefinition.class), execution);

    // then:
    assertThat(result.getNextNodes().size()).isEqualTo(1);
    assertThat(result.getNextNodes().get(0).getId()).isEqualTo(id(TASK));
  }

  @Test
  public void shouldExecuteChoiceFalsePath() {
    // given:
    ChoiceNodeExecutor choiceNodeExecutor = createChoiceNodeExecutor();

    FlowDefinition flowDefinition = choiceFlow();
    Trigger trigger = new Trigger().property("foo", "oof");
    DefaultExecutionContext execution = new DefaultExecutionContext(trigger, registryWithServiceMock());

    // when:
    FlowNodeExecutionResult result = choiceNodeExecutor.execute(flowDefinition.getNode(id(CHOICE), ChoiceDefinition.class), execution);

    // then:
    assertThat(result.getNextNodes().size()).isEqualTo(1);
    assertThat(result.getNextNodes().get(0).getId()).isEqualTo(id(TASK2));
  }

  @Test
  public void shouldEvaluatePropertyPredicate() {
    // given:
    BrainslugContext brainslugContext = new BrainslugContextBuilder().build();
    FlowNodeExecutor<ChoiceDefinition> choiceNodeExecutor = new ChoiceNodeExecutor(predicateEvaluator).withTokenOperations(new TokenOperations(tokenStore));
    DefaultExecutionContext executionContext = new DefaultExecutionContext(new Trigger(), registryWithServiceMock());

    FlowDefinition flowDefinition = propertyPredicateFlow(true);
    // when:
    FlowNodeExecutionResult executionResult = choiceNodeExecutor.execute(flowDefinition.getNode(IdUtil.id("choice"), ChoiceDefinition.class), executionContext);

    // then:
    assertThat(executionResult.getNextNodes())
      .hasSize(1)
      .contains(flowDefinition.getNode(id("task")));
  }

  @Test
  public void shouldTakeOtherwisePathIfNoneMatches() {
    // given:
    FlowNodeExecutor<ChoiceDefinition> choiceNodeExecutor = new ChoiceNodeExecutor(predicateEvaluator).withTokenOperations(new TokenOperations(tokenStore));
    DefaultExecutionContext executionContext = new DefaultExecutionContext(new Trigger(), registryWithServiceMock());

    FlowDefinition flowDefinition = propertyPredicateFlow(false);

    // when:
    FlowNodeExecutionResult executionResult = choiceNodeExecutor.execute(flowDefinition.getNode(IdUtil.id("choice"), ChoiceDefinition.class), executionContext);

    // then:
    assertThat(executionResult.getNextNodes())
      .hasSize(1)
      .containsOnly(flowDefinition.getNode(id("end")));
  }

  ChoiceNodeExecutor createChoiceNodeExecutor() {
    PredicateEvaluator mock = new DefaultPredicateEvaluator();
    TokenOperations tokenOperations = mock(TokenOperations.class);

    return new ChoiceNodeExecutor(mock)
      .withTokenOperations(tokenOperations);
  }

  private FlowDefinition propertyPredicateFlow(final boolean predicateFulfilled) {
    return new FlowBuilder(){
      @Override
      public void define() {
        start(id("start")).choice(id("choice"))
          .when(predicate(new PropertyPredicate() {
            @Override
            public boolean isFulfilled(ExecutionProperties executionProperties) {
              return predicateFulfilled;
            }
          })).then().execute(task(id("task")))
        .otherwise().end(id("end"));
      }
    }.getDefinition();
  }

  private BrainslugContext contextWithChoiceFlow() {
    FlowDefinition definition = choiceFlow();

    context.addFlowDefinition(definition);
    return spy(context);
  }

  private FlowDefinition choiceFlow() {
    return new FlowBuilder() {
        @Override
        public void define() {
          start(event(id(START))).choice(id(CHOICE))
            .when(eq(property(id("foo")), "bar")).execute(task(id(TASK)))
            .or()
            .when(eq(property(id("foo")), "oof")).execute(task(id(TASK2)))
            .otherwise()
            .end(id("otherwise"));
        }

        @Override
        public String getId() {
          return CHOICEID.name();
        }

      }.getDefinition();
  }

}
