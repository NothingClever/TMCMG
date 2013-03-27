package togos.noise.v3.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import togos.lang.ParseError;
import togos.noise.v3.parser.ast.ASTNode;
import togos.noise.v3.parser.ast.InfixNode;
import togos.noise.v3.parser.ast.ParenApplicationNode;
import togos.noise.v3.parser.ast.SymbolNode;
import togos.noise.v3.program.structure.ArgumentList;
import togos.noise.v3.program.structure.Block;
import togos.noise.v3.program.structure.FunctionApplication;
import togos.noise.v3.program.structure.SymbolReference;
import togos.noise.v3.program.structure.Expression;

public class ProgramTreeBuilder
{
	protected void flatten(ASTNode n, String operator, List<ASTNode> dest) {
		if( n instanceof InfixNode && operator.equals(((InfixNode)n).operator) ) {
			flatten( (InfixNode)n, dest );
		} else {
			dest.add(n);
		}
	}
	
	protected void flatten(InfixNode n, List<ASTNode> dest) {
		flatten( n.n1, n.operator, dest );
		flatten( n.n2, n.operator, dest );
	}
	
	protected List<ASTNode> flatten( InfixNode n ) {
		ArrayList<ASTNode> l = new ArrayList<ASTNode>();
		flatten( n, l );
		return l;
	}
	
	protected Block<Object> parseBlock( InfixNode ast ) throws ParseError {
		assert ";".equals(ast.operator);
		HashMap<String,Expression<Object>> definitions = new HashMap<String,Expression<Object>>();
		ASTNode blockValueNode = null;
		List<ASTNode> blockParts = flatten(ast);
		for( ASTNode bp : blockParts ) {
			if( bp instanceof InfixNode && "=".equals(((InfixNode)bp).operator) ) {
				InfixNode defOp = (InfixNode)bp;
				String defName;
				ASTNode defValue;
				if( defOp.n1 instanceof SymbolNode ) {
					defName = ((SymbolNode)defOp.n1).text;
					defValue = defOp.n2;
				} if( defOp.n1 instanceof ParenApplicationNode ) {
					ASTNode defFunNameNode = ((ParenApplicationNode)defOp.n1).function;
					if( !(defFunNameNode instanceof SymbolNode) ) {
						throw new ParseError("Defined function name must be a symbol", defFunNameNode);
					}
					defName = ((SymbolNode)defFunNameNode).text;
					defValue = new InfixNode("->", ((ParenApplicationNode)defOp.n1).argumentList, defOp.n2, defOp);
				} else {
					throw new ParseError("Invalid lvalue for definition", defOp.n1);
				}
				definitions.put(defName, parseExpression(defValue));
			} else {
				if( blockValueNode != null ) {
					throw new ParseError("More than one value defined for block", bp);
				}
				blockValueNode = bp;
			}
		}
		if( blockValueNode == null ) {
			throw new ParseError("Block has no value", ast);
		}
		return new Block<Object>( definitions, parseExpression(blockValueNode), ast );
	}
	
	public Expression<Object> parseExpression( ASTNode ast ) throws ParseError {
		if( ast instanceof InfixNode ) {
			InfixNode opApp = (InfixNode)ast;
			if( ";".equals(opApp.operator) ) {
				return parseBlock(opApp);
			} else if( ",".equals(opApp.operator) ) {
				throw new ParseError("Comma not allowed, here", ast);
			} else {
				return new FunctionApplication(
					new SymbolReference(opApp.operator, opApp),
					new ArgumentList(parseExpression(opApp.n1), parseExpression(opApp.n2), opApp),
					opApp
				);
			}
		} else if( ast instanceof SymbolNode ) {
			// TODO: parse numbers and stuff!
			return new SymbolReference( ((SymbolNode)ast).text, ast );
		} else {
			throw new ParseError("Don't know how to parse this "+ast.getClass(), ast);
		}
	}
}