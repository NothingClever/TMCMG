package togos.noise.v3.parser.ast;

import togos.lang.SourceLocation;

public class FunctionApplication extends ASTNode
{
	public final ASTNode function;
	public final ASTNode argumentList;
	
	public FunctionApplication( ASTNode function, ASTNode args, SourceLocation sLoc ) {
		super(sLoc);
		this.function = function;
		this.argumentList = args;
	}
	
	public String toString() {
		return function.toString() + "(" + argumentList.toString() + ")";
	}
	
	public String toAtomicString() { return toString(); }
}
