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

import java.util.Arrays;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.Messenger;

public class ClojureScriptFactoryTests extends TestCase {
	
	public void testStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("clojureContext.xml", getClass());

		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator"));
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger"));
//
		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		System.out.println(messenger.getMessage());
//
//		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
//		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);
//
		assertEquals(calc, calc);
//		assertEquals(messenger, messenger);
//		assertTrue(!messenger.equals(calc));
//		assertTrue(messenger.hashCode() != calc.hashCode());
//		assertTrue(!messenger.toString().equals(calc.toString()));
//
		assertEquals(5, calc.add(2, 3));
//
//		String desiredMessage = "Hello World!";
//		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
//
//		assertTrue(ctx.getBeansOfType(Calculator.class).values().contains(calc));
//		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}
	
}
