package net.fmjaeschke.quantumhealth;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Structural guard: a use-case method that receives a {@link ResourceId} must consult the
 * {@link AccessPolicy} — either via {@code check(...)} for instance-level enforcement, or via
 * {@code isDoctor()} for the scoping/ownership variants. A newly added guard method that forgets
 * to consult the policy fails this test, instead of silently shipping an authorization gap.
 */
@AnalyzeClasses(packages = "net.fmjaeschke.quantumhealth", importOptions = ImportOption.DoNotIncludeTests.class)
class AccessPolicyEnforcementTest {

    /**
     * Creation flows reference a parent {@link ResourceId} (e.g. the patient) only to build a new
     * entity; they are intentionally not resource-level guarded here (role gating happens at the
     * REST boundary). Adding a method to this set is a deliberate, reviewable act.
     */
    private static final Set<String> CREATION_METHODS_WITHOUT_RESOURCE_CHECK = Set.of("schedule", "issue");

    @ArchTest
    static final ArchRule use_case_methods_taking_a_resource_id_must_consult_access_policy =
            methods().that(arePublicUseCaseMethodsTakingAResourceId())
                    .should(consultAccessPolicy())
                    .because("a use-case method that receives a ResourceId must consult AccessPolicy "
                            + "(check(), isDoctor() or mayAccessOwnedBy()); otherwise a forgotten call silently ships an authorization gap");

    private static DescribedPredicate<JavaMethod> arePublicUseCaseMethodsTakingAResourceId() {
        return DescribedPredicate.describe(
                "public use-case methods that take a ResourceId parameter (excluding creation methods "
                        + CREATION_METHODS_WITHOUT_RESOURCE_CHECK + ")",
                method -> method.getModifiers().contains(JavaModifier.PUBLIC)
                        && method.getOwner().getPackageName().contains(".application.usecase")
                        && method.getRawParameterTypes().stream().anyMatch(p -> p.isAssignableTo(ResourceId.class))
                        && !CREATION_METHODS_WITHOUT_RESOURCE_CHECK.contains(method.getName()));
    }

    private static ArchCondition<JavaMethod> consultAccessPolicy() {
        return new ArchCondition<>("consult AccessPolicy via check(), isDoctor() or mayAccessOwnedBy()") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean consults = method.getMethodCallsFromSelf().stream().anyMatch(
                        call -> targetsAccessPolicy(call) && isPolicyConsultation(call.getName()));
                if (!consults) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " takes a ResourceId but never consults AccessPolicy (check/isDoctor/mayAccessOwnedBy)"));
                }
            }
        };
    }

    private static boolean targetsAccessPolicy(JavaMethodCall call) {
        return call.getTarget().getOwner().isAssignableTo(AccessPolicy.class);
    }

    private static boolean isPolicyConsultation(String methodName) {
        return methodName.equals("check")
                || methodName.equals("isDoctor")
                || methodName.equals("mayAccessOwnedBy");
    }
}
