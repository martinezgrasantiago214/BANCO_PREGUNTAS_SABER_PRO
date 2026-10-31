package co.unicauca.saberpro.outbox;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Hace transaccional cada metodo publico de los servicios de aplicacion
 * <b>sin</b> anotarlos con Spring: la capa {@code application} sigue siendo
 * Java puro y la frontera transaccional se declara en {@code infrastructure}.
 *
 * <p>Es lo que permite que guardar el agregado y escribir sus eventos en la
 * outbox ocurran en una sola transaccion. Los metodos de consulta
 * ({@code find*}, {@code get*}, {@code list*}, {@code search*}) se ejecutan
 * en modo solo lectura.</p>
 *
 * <p>Uso en una clase {@code @Configuration}:</p>
 * <pre>
 * &#64;Bean &#64;Role(BeanDefinition.ROLE_INFRASTRUCTURE)
 * static Advisor useCaseTransactions(TransactionManager tm) {
 *     return UseCaseTransactions.advisor(tm, QuestionService.class);
 * }
 * </pre>
 */
public final class UseCaseTransactions {

    private static final List<String> READ_ONLY_PREFIXES = List.of("find", "get", "list", "search");

    private UseCaseTransactions() {
    }

    public static Advisor advisor(TransactionManager transactionManager, Class<?>... useCaseClasses) {
        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        RuleBasedTransactionAttribute readOnly = rollbackOnAnyException();
        readOnly.setReadOnly(true);
        READ_ONLY_PREFIXES.forEach(prefix -> source.addTransactionalMethod(prefix + "*", readOnly));
        source.addTransactionalMethod("*", rollbackOnAnyException());

        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager, source);
        return new DefaultPointcutAdvisor(new PublicMethodsOf(useCaseClasses), interceptor);
    }

    private static RuleBasedTransactionAttribute rollbackOnAnyException() {
        RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
        attribute.setRollbackRules(List.of(new RollbackRuleAttribute(Exception.class)));
        return attribute;
    }

    private static final class PublicMethodsOf extends StaticMethodMatcherPointcut {

        private final List<Class<?>> classes;

        PublicMethodsOf(Class<?>... classes) {
            this.classes = Arrays.asList(classes);
            setClassFilter(clazz -> this.classes.stream().anyMatch(c -> c.isAssignableFrom(clazz)));
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return Modifier.isPublic(method.getModifiers())
                    && classes.contains(method.getDeclaringClass());
        }
    }
}
