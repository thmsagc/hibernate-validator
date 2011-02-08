/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.validator.test.group;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.GroupDefinitionException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.group.DefaultGroupSequenceProvider;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodValidator;
import org.hibernate.validator.test.group.model.User;

import static org.hibernate.validator.test.util.TestUtil.assertCorrectConstraintViolationMessages;
import static org.hibernate.validator.test.util.TestUtil.assertNumberOfViolations;
import static org.hibernate.validator.test.util.TestUtil.getValidator;

/**
 * @author Kevin Pollet - SERLI - (kevin.pollet@serli.com)
 */
public class DefaultGroupSequenceProviderTest {

	private static Validator validator;

	private static MethodValidator methodValidator;

	@BeforeClass
	public static void init() {
		validator = getValidator();
		methodValidator = validator.unwrap( MethodValidator.class );
	}

	@Test
	public void testNullProviderDefaultGroupSequence() {
		Set<ConstraintViolation<A>> violations = validator.validate( new A() );

		assertNumberOfViolations( violations, 1 );
	}

	@Test(expectedExceptions = GroupDefinitionException.class)
	public void testNotValidProviderDefaultGroupSequenceDefinition() {
		validator.validate( new B() );
	}

	@Test
	public void testValidateUserProviderDefaultGroupSequence() {
		User user = new User( "wrong$$password" );
		Set<ConstraintViolation<User>> violations = validator.validate( user );

		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "must match \"\\w+\"" );

		User admin = new User( "short", true );
		violations = validator.validate( admin );

		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "length must be between 10 and 20" );
	}

	@Test
	public void testValidatePropertyUserProviderDefaultGroupSequence() {
		User user = new User( "wrong$$password" );
		Set<ConstraintViolation<User>> violations = validator.validateProperty( user, "password" );

		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "must match \"\\w+\"" );

		User admin = new User( "short", true );
		violations = validator.validateProperty( admin, "password" );

		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "length must be between 10 and 20" );
	}

	@Test
	public void testValidateReturnValueProviderDefaultGroupSequence() throws NoSuchMethodException {
		C c = new CImpl();
		Method fooMethod = C.class.getDeclaredMethod( "foo", String.class );

		Set<MethodConstraintViolation<C>> violations = methodValidator.validateReturnValue(
				c, fooMethod, c.foo( null )
		);
		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "may not be null" );

		violations = methodValidator.validateReturnValue( c, fooMethod, c.foo( "foo" ) );
		assertNumberOfViolations( violations, 1 );
		assertCorrectConstraintViolationMessages( violations, "length must be between 10 and 20" );
	}

	@GroupSequenceProvider(NullGroupSequenceProvider.class)
	static class A {

		@NotNull
		String c;

		@NotNull(groups = TestGroup.class)
		String d;

	}

	@GroupSequenceProvider(InvalidGroupSequenceProvider.class)
	static class B {

	}

	@GroupSequenceProvider(MethodGroupSequenceProvider.class)
	static interface C {

		@NotNull(message = "may not be null")
		@Length(min = 10, max = 20, groups = TestGroup.class, message = "length must be between {min} and {max}")
		public String foo(String param);

	}

	static class CImpl implements C {

		public String foo(String param) {
			return param;
		}

	}

	interface TestGroup {

	}

	public static class MethodGroupSequenceProvider implements DefaultGroupSequenceProvider<C> {

		public List<Class<?>> getValidationGroups(C object) {
			List<Class<?>> defaultGroupSequence = new ArrayList<Class<?>>();
			defaultGroupSequence.add( TestGroup.class );
			defaultGroupSequence.add( C.class );

			return defaultGroupSequence;
		}
	}

	public static class NullGroupSequenceProvider implements DefaultGroupSequenceProvider<A> {

		public List<Class<?>> getValidationGroups(A object) {
			return null;
		}

	}

	public static class InvalidGroupSequenceProvider implements DefaultGroupSequenceProvider<B> {

		public List<Class<?>> getValidationGroups(B object) {
			List<Class<?>> defaultGroupSequence = new ArrayList<Class<?>>();
			defaultGroupSequence.add( TestGroup.class );

			return defaultGroupSequence;
		}

	}

}
