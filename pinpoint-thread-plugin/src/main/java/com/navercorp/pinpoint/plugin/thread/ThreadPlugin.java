package com.navercorp.pinpoint.plugin.thread;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matcher;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matchers;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.operand.InterfaceInternalNameMatcherOperand;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.MatchableTransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.plugin.thread.interceptor.ThreadCallInterceptor;
import com.navercorp.pinpoint.plugin.thread.interceptor.ThreadConstructorInterceptor;

import java.security.ProtectionDomain;
import java.util.List;

/**
 * @author echo
 * <p>
 * this plugin for record async thread trace
 */
public class ThreadPlugin implements ProfilerPlugin, MatchableTransformTemplateAware {

    private static final PLogger logger = PLoggerFactory.getLogger(ThreadPlugin.class);

    private MatchableTransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ThreadConfig threadConfig = new ThreadConfig(context.getConfig());

        logger.info("ThreadPlugin init {},config:{}", this.getClass().getSimpleName(), threadConfig);
        String threadMatchPackages = threadConfig.getThreadMatchPackage();
        if (StringUtils.isEmpty(threadMatchPackages)) {
            logger.info("thread plugin package is empty,skip it");
            return;
        }

        List<String> threadMatchPackageList = StringUtils.tokenizeToStringList(threadMatchPackages, ",");
        for (String threadMatchPackage : threadMatchPackageList) {
            logger.info("threadMatchPackage: {}", threadMatchPackage);
            addRunnableInterceptor(threadMatchPackage);
            addCallableInterceptor(threadMatchPackage);
        }
    }

    private void addRunnableInterceptor(String threadMatchPackage) {
        Matcher matcher = Matchers.newPackageBasedMatcher(threadMatchPackage, new InterfaceInternalNameMatcherOperand("java.lang.Runnable", true));
        transformTemplate.transform(matcher, RunnableTransformCallback.class);
    }

    @SuppressWarnings("all")
    public static class RunnableTransformCallback implements TransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, protectionDomain, classfileBuffer);
            logger.info("ThreadPlugin targetName: {}", target.getName());

            List<InstrumentMethod> allConstructor = target.getDeclaredConstructors();
            for (int i = 0; i < allConstructor.size(); i++) {
                InstrumentMethod instrumentMethod = allConstructor.get(i);
                logger.info("ThreadPlugin className: {} instrumentMethod: {}", className, instrumentMethod);
                instrumentMethod.addScopedInterceptor(ThreadConstructorInterceptor.class, ThreadConstants.SCOPE_NAME);
            }
            target.addField(AsyncContextAccessor.class);
            target.addField(ZeyeTraceIdAccessor.class.getName());
            final InstrumentMethod runMethod = target.getDeclaredMethod("run");
            if (runMethod != null) {
                logger.info("runMethod: {}", runMethod.getName());
                runMethod.addInterceptor(ThreadCallInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    private void addCallableInterceptor(String threadMatchPackage) {
        Matcher matcher = Matchers.newPackageBasedMatcher(threadMatchPackage, new InterfaceInternalNameMatcherOperand("java.util.concurrent.Callable", true));
        transformTemplate.transform(matcher, CallableTransformCallback.class);
    }

    @SuppressWarnings("all")
    public static class CallableTransformCallback implements TransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, protectionDomain, classfileBuffer);
            List<InstrumentMethod> allConstructor = target.getDeclaredConstructors();
            for (int i = 0; i < allConstructor.size(); i++) {
                InstrumentMethod instrumentMethod = allConstructor.get(i);
                logger.info("ThreadPlugin className: {} instrumentMethod: {}", className, instrumentMethod);
                instrumentMethod.addScopedInterceptor(ThreadConstructorInterceptor.class, ThreadConstants.SCOPE_NAME);
            }
            target.addField(AsyncContextAccessor.class);
            final InstrumentMethod callMethod = target.getDeclaredMethod("call");
            if (callMethod != null) {
                logger.info("callMethod: {}", callMethod.getName());
                callMethod.addInterceptor(ThreadCallInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    @Override
    public void setTransformTemplate(MatchableTransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
