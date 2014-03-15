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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.bsh.BshScriptUtils;
import org.springframework.util.Assert;

import clojure.java.api.Clojure;
import clojure.lang.Compiler;
import clojure.lang.IFn;


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
public class ClojureScriptFactory implements ScriptFactory {
	
	private final String scriptSourceLocator;

	public ClojureScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
	}
	
	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	public Class<?>[] getScriptInterfaces() {
		return null;
	}

	/**
	 * Clojure scripts do not require a config interface.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return false;
	}
	
	@Override
	public Object getScriptedObject(ScriptSource scriptSource,
			Class<?>... actualInterfaces) throws IOException, ScriptCompilationException {
		return getScriptedObject(scriptSource, null, actualInterfaces);
	}

	@Override
	public Object getScriptedObject(ScriptSource scriptSource, MutablePropertyValues immutableBindings,
			Class<?>... actualInterfaces) throws IOException, ScriptCompilationException {
		try {
			String scriptAsString = scriptSource.getScriptAsString();
			Clojure.read(scriptAsString);
			
			// bindings
			final Map<String, Object> immutableProperties = new HashMap<String, Object>();
			
			if (immutableBindings != null) {
				for(PropertyValue pv : immutableBindings.getPropertyValueList()) {
					immutableProperties.put(pv.getName(), pv.isConverted() ? pv.getConvertedValue() : pv.getValue());
				}
				
				IFn bindingFunction = (IFn) Compiler.load(new StringReader("(fn [bindings] (intern 'clojure.core '*spring-bindings* (into {} bindings)))"));
				bindingFunction.invoke(immutableProperties);
			}
			
			// script
			return Compiler.load(new StringReader(scriptAsString));
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

}
