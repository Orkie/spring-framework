/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting.clojure;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.bsh.BshScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import clojure.java.api.Clojure;
import clojure.lang.Compiler;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;


/**
 * {@link org.springframework.scripting.ScriptFactory} implementation
 * for a Clojure script.
 *
 * <p>Typically used in combination with a
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * <p>Note: Spring 4.0 supports Clojure 1.6 and higher.
 *
 * @author Adan Scotney
 * @since 4.0
 * @see BshScriptUtils
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 */
public class ClojureScriptFactory implements ScriptFactory, BeanClassLoaderAware {
	
	private final String scriptSourceLocator;
	
	private final Class<?>[] scriptInterfaces;
	
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	public ClojureScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = null;
	}
	
	public ClojureScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	public Class<?>[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	/**
	 * Clojure scripts do require a config interface.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return true;
	}

	@Override
	public Object getScriptedObject(ScriptSource scriptSource,
			Class<?>... actualInterfaces) throws IOException, ScriptCompilationException {
		try {
			String scriptAsString = scriptSource.getScriptAsString();
			Clojure.read(scriptAsString);
			Object loadResult = Compiler.load(new StringReader(scriptAsString));
			return Proxy.newProxyInstance(beanClassLoader, actualInterfaces, new ClojureObjectInvocationHandler(loadResult));
		} catch(RuntimeException ex) {
			String msg = (ex != null && ex.getMessage() != null) ?
					ex.getMessage().toString() : "Unexpected Clojure error";
			throw new ScriptCompilationException(scriptSource, msg, ex);
		}
	}

	@Override
	public Class<?> getScriptedObjectType(ScriptSource scriptSource) throws IOException,
			ScriptCompilationException {
		return null;
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		return scriptSource.isModified();
	}
	
	@Override
	public String toString() {
		return "ClojureScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}
	
	public static class ClojureObjectInvocationHandler implements InvocationHandler {
		
		private final Object loadResult;
		
		public ClojureObjectInvocationHandler(Object loadResult) {
			this.loadResult = loadResult;
		}

		/* (non-Javadoc)
		 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			
			if(isSetter(method)) {
				PropertyDescriptor property = BeanUtils.findPropertyForMethod(method);
				String qualifiedVarName = property.getName().substring("#'".length());
				IFn var;
				try {
					var = Clojure.var(qualifiedVarName);
				} catch(NullPointerException ex) {
					throw new IllegalArgumentException("Couldn't find Clojure var '"+qualifiedVarName+"'");
				}
				var.invoke(args[0]);
				return null;
			}
			
			return method.invoke(loadResult, args);
		}
		
		// Could this be better if we somehow captured the point at which the config proxy is created, looked at the declaring 
		// class of the method, and used that too? Could it be stashed in the bean attributes or something?
		private boolean isSetter(Method method) {
			if(!method.getName().startsWith("set#'")) return false;
			else if(!method.getReturnType().equals(Void.TYPE)) return false;
			else if(method.getParameterCount() != 1) return false;
			return true;
		}
	}

}
