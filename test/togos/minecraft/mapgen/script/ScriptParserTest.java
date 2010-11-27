package togos.minecraft.mapgen.script;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;

public class ScriptParserTest extends TestCase
{
	protected ScriptNode parse(String str) {
		try {
			StringReader sr = new StringReader(str);
			return new ScriptParser(sr).readNode(0);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void testParseAtom() {
		ScriptNode expected = new ScriptNode("foo", Collections.EMPTY_LIST);
		assertEquals( expected, parse("foo") );
	}
	
	public void testParseSingleArgument() {
		ArrayList expectedArguments = new ArrayList();
		expectedArguments.add( new ScriptNode("bar") );
		ScriptNode expected = new ScriptNode("foo", expectedArguments);
		assertEquals( expected, parse("foo( bar )") );
	}
	
	public void testParseMultiArguments() {
		ArrayList expectedArguments = new ArrayList();
		expectedArguments.add( new ScriptNode("bar") );
		expectedArguments.add( new ScriptNode("baz") );
		ScriptNode expected = new ScriptNode("foo", expectedArguments);
		assertEquals( expected, parse("foo( bar, baz )") );
	}
	
	public void testParseOperator() {
		ArrayList expectedArguments = new ArrayList();
		expectedArguments.add( new ScriptNode("foo") );
		expectedArguments.add( new ScriptNode("bar") );
		ScriptNode expected = new ScriptNode("+", expectedArguments);
		assertEquals( expected, parse("foo + bar") );
	}
	
	public void testParseMultiPrecedence() {
		ArrayList ea1 = new ArrayList();
		ea1.add( new ScriptNode("foo") );
		ea1.add( new ScriptNode("bar") );
		ScriptNode e1 = new ScriptNode("+", ea1);

		ArrayList ea2 = new ArrayList();
		ea2.add( new ScriptNode("baz") );
		ea2.add( new ScriptNode("quux") );
		ScriptNode e2 = new ScriptNode("*", ea2);

		ArrayList ea3 = new ArrayList();
		ea3.add( e1 );
		ea3.add( e2 );
		ScriptNode e3 = new ScriptNode("/", ea3);

		assertEquals( e3, parse("(foo + bar) / baz * quux") );
	}

	public void testParseMultiPrecedence2() {
		ArrayList ea1 = new ArrayList();
		ea1.add( new ScriptNode("foo") );
		ea1.add( new ScriptNode("bar") );
		ScriptNode e1 = new ScriptNode("**", ea1);

		ArrayList ea2 = new ArrayList();
		ea2.add( new ScriptNode("baz") );
		ea2.add( new ScriptNode("quux") );
		ea2.add( new ScriptNode("grawr") );
		ScriptNode e2 = new ScriptNode("*", ea2);

		ArrayList ea3 = new ArrayList();
		ea3.add( e1 );
		ea3.add( e2 );
		ea3.add( new ScriptNode("thing") );
		ScriptNode e3 = new ScriptNode("/", ea3);

		assertEquals( e3, parse("foo ** bar / baz * quux * grawr / thing") );
	}
	
	public void testParseEquals() {
		ArrayList ea1 = new ArrayList();
		ea1.add( new ScriptNode("cow") );
		ea1.add( new ScriptNode("pig") );
		ScriptNode e1 = new ScriptNode("=", ea1);
		
		assertEquals( e1, parse("cow = pig") );
	}
	
	public void testParseEqualsAndSemiColinz() {
		ArrayList ea1 = new ArrayList();
		ea1.add( new ScriptNode("cow") );
		ea1.add( new ScriptNode("pig") );
		ScriptNode e1 = new ScriptNode("=", ea1);
		
		ArrayList ea2 = new ArrayList();
		ea2.add( new ScriptNode("horse") );
		ea2.add( new ScriptNode("rabbit") );
		ScriptNode e2 = new ScriptNode("=", ea2);
		
		ArrayList ea3 = new ArrayList();
		ea3.add(e1);
		ea3.add(e2);
		ScriptNode e3 = new ScriptNode(";",ea3);
		
		assertEquals( e3, parse("cow = pig; horse = rabbit") );
	}
	
	public void testDifferentSyntax() {
		assertEquals(
			parse("peace = war; freedom = slavery; strength = ignorance"),
			parse(";(=(peace,war), =(freedom,slavery), =(strength,ignorance))")
		);
	}
}
